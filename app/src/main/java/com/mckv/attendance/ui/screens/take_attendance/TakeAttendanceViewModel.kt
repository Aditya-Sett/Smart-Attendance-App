package com.mckv.attendance.ui.screens.take_attendance

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mckv.attendance.data.local.ActiveCodeManager
import com.mckv.attendance.data.remote.RetrofitClient
import com.mckv.attendance.data.remote.api.AttendanceCodeModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class TakeAttendanceViewModel(application: Application): AndroidViewModel(application){
    //This is the private state that the ViewModel updates
    private val _uiState= MutableStateFlow<AttendanceUiState>(AttendanceUiState.Idle)
    // This is what the UI observes
    val uiState: StateFlow<AttendanceUiState> = _uiState

    // Keep track of active codes locally so the UI can display multiple cards
    // Instead of List<JSONObject>
    private val _activeAttendanceList = MutableStateFlow<List<AttendanceCodeModel>>(emptyList())
    val activeAttendanceList: StateFlow<List<AttendanceCodeModel>> = _activeAttendanceList

    private var hasInitialFetchBeenDone = false

    //ERROR SHOWING CHANNEL IN TOAST
    private val _eventChannel= Channel<String>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    //FUNCTION TO FETCH CURRENT CLASS
    fun fetchCurrentClass(teacherId: String){

        // ALLOW fetching if Idle OR if a Session is already running
        val canFetch = _uiState.value is AttendanceUiState.Idle || _uiState.value is AttendanceUiState.ActiveSessionState

        if (!canFetch || hasInitialFetchBeenDone) return

        viewModelScope.launch {
            //We don't want to overwrite 'ActiveSessionState' with a global 'Loading'
            // because it would hide the active timer card.
            // Only set  Loading if we are currently Idle.
            if (_uiState.value is AttendanceUiState.Idle) {
                _uiState.value = AttendanceUiState.Loading
            }

            try {
                val requestBody= buildRequestBodyForCurrentClass(teacherId)

                val response= RetrofitClient.analysisInstance.getCurrentClass2(requestBody)

                if (response.isSuccessful && response.body() != null && response.body()?.success == true) {

                    hasInitialFetchBeenDone = true

                    val data = response.body()!!
                    _uiState.value = AttendanceUiState.FetchCurrentClassSuccess(
                        department = data.department,
                        subject = data.subject,
                        semester = formatSemester(data.semester)
                    )
                }else{
                    _eventChannel.send("NO CLASS FOUND")

                    _uiState.value= AttendanceUiState.Idle
                }

            }catch (e: Exception){

                _eventChannel.send("NETWORK ERROR")

                _uiState.value= AttendanceUiState.Idle
            }
        }
    }

    fun generateAttendanceCode(
        teacherId: String,
        department: String,
        subject: String,
        wifiFingerprint: JSONArray,
        sem: String
    ) {
        viewModelScope.launch {
            _uiState.value = AttendanceUiState.LoadingGenerateAttendance
            try {

                val uuid = java.util.UUID.randomUUID().toString()

                val requestBody = buildRequestBodyForGenerateAttendanceCode(
                    teacherId, department, subject, wifiFingerprint, sem, uuid
                )

                val response = RetrofitClient.instance.genearteCode2(requestBody)

                if (response.isSuccessful && response.body() != null) {
                    // FIX: Convert response body to string first
                    val data = response.body()!!

                    if (data.success) {

                        _uiState.value= AttendanceUiState.ActiveSessionState

                        val expiryString = data.expiresAt
                        val durationMillis = parseExpiryStringToMillis(expiryString)

                        ActiveCodeManager.saveActiveCode(getApplication(), data)

                        // 3. Start BLE Service with Long duration
                        startBackgroundAdvertising(uuid, durationMillis)

                        // THIS WILL STOR CODE IN STACK LIK
                        _activeAttendanceList.value = listOf(data) + _activeAttendanceList.value
                    }
                }else{
                    _eventChannel.send("FAILED TO GENERATE")

                    _uiState.value = AttendanceUiState.Idle
                }
            } catch (e: Exception) {
                _eventChannel.send("NETWORK ERROR")

                _uiState.value = AttendanceUiState.Idle
            }
        }
    }

    fun deleteCode(attendance: AttendanceCodeModel){
        viewModelScope.launch {
            _uiState.value= AttendanceUiState.LoadingDeleteCode
            try {
                val requestBody= buildRequestBodyForDeleteCode(attendance)

                val response= RetrofitClient.instance.deleteCode(requestBody)

                if(response.isSuccessful && response.body()!=null && response.body()?.success==true){

                    // 2. Clear from Local Cache (SharedPreferences)
                    ActiveCodeManager.clearActiveCode(getApplication())

                    _activeAttendanceList.value = _activeAttendanceList.value.filter { it.code.toString() != attendance.code.toString() }

                    // 4. Stop the Bluetooth Service
                    stopBleService()

                    _uiState.value= AttendanceUiState.Idle

                }else{
                    _eventChannel.send("FAILED TO DELETE CODE")

                    _uiState.value = AttendanceUiState.Idle
                }
            }catch (e: Exception){
                _eventChannel.send("NETWORK ERROR")

                _uiState.value = AttendanceUiState.Idle
            }
        }
    }

    //THIS WILL BE LOAD EVERY TIME THE UI RENDER
    fun loadSavedActiveCode() {
        val savedCode = ActiveCodeManager.getActiveCode(getApplication())

        //CODE IS FOUND IN LOCAL STORAGE
        if (savedCode != null) {
            // 1. Check if it's already expired
            val format = SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.getDefault())
            val expiryTime = try { format.parse(savedCode.expiresAt)?.time ?: 0L } catch (e: Exception) { 0L }

            //VALID CODE
            if (expiryTime > System.currentTimeMillis()) {
                _uiState.value = AttendanceUiState.ActiveSessionState

                val currentList = _activeAttendanceList.value.filter { it.code != savedCode.code }
                _activeAttendanceList.value = listOf(savedCode) + currentList
            } else {
                //EXPIRED CODE , CLEAR THE LOCAL STORAGE
                ActiveCodeManager.clearActiveCode(getApplication())
                _uiState.value= AttendanceUiState.Idle
            }

            if (_uiState.value is AttendanceUiState.LoadingGenerateAttendance) {
                _uiState.value = AttendanceUiState.Idle
            }
        } else {
            // NO CODE ACTIVE
            _uiState.value= AttendanceUiState.Idle
        }
    }

    //START BACKGROUND BROADCASTING
    fun startBackgroundAdvertising(uuid: String, duration: Long) {

        // Use getApplication() instead of a passed-in context
        val context = getApplication<android.app.Application>()

        val intent = Intent(context, BleAdvertisingService::class.java).apply {
            putExtra("UUID", uuid)
            putExtra("DURATION", duration)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)

            println("expires ast: $duration and UUID: $uuid")
        } else {
            context.startService(intent)
        }
    }

    //STOP BLE SERVIC
    private fun stopBleService() {
        val context = getApplication<Application>()
        val intent = Intent(context, BleAdvertisingService::class.java)
        context.stopService(intent)
    }

    //RUN WHEN BLUETOOTH DISABLED RUNNING CODE
    fun handleBluetoothDisabled() {
        val currentActiveCode = _activeAttendanceList.value.firstOrNull()

        //DELETE THAT CODE FROM SERVER
        currentActiveCode?.let {
                attendance-> deleteCode(attendance)
        }

        //CLEAR CODE FROM LOCAL STORA
        ActiveCodeManager.clearActiveCode(getApplication())

        //CLEAR LIS
        _activeAttendanceList.value = emptyList()

        //STOP BLE SERVICE
        stopBleService()

        viewModelScope.launch {
            _eventChannel.send("BLUETOOTH DISABLED -> CODE CANCELED")
        }

        _uiState.value= AttendanceUiState.Idle
    }

    //THIS WILL TRIGGER WHEN CODE EXPIRED
    fun handleSessionExpiration() {
        viewModelScope.launch {
            delay(2000)

            //CLEAR LIS
            _activeAttendanceList.value = emptyList()

            //CLEAR THE CODE FROM LOCAL STORAGE
            ActiveCodeManager.clearActiveCode(getApplication())

            // When the timer hits zero, we tell the UI the session is over.
            _uiState.value = AttendanceUiState.Idle

        }
    }

    //_________________________________________________________________________________________________

    //BUILD REQUEST BODY FOR GENERATE ATTENDANCE CODE
    private fun buildRequestBodyForGenerateAttendanceCode(
        teacherId: String,
        department: String,
        subject: String,
        wifiFingerprint: JSONArray,
        sem: String,
        bluetoothUuid: String?
    ): RequestBody {

        return JSONObject().apply {

            put("teacherId", teacherId)
            put("department", department)
            put("subject", subject)
            put("wifiFingerprint", wifiFingerprint)
            put("sem", sem)
            put("bluetoothUuid", bluetoothUuid)

        }.toString().toRequestBody("application/json".toMediaTypeOrNull())

    }

    // 1. Helper to parse your String date into Long Milliseconds
    private fun parseExpiryStringToMillis(expiryString: String): Long {
        return try {
            // MATCH THIS PATTERN TO YOUR BACKEND (e.g., "dd-MM-yyyy hh:mm:ss a")
            val sdf = SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.getDefault())
            val date = sdf.parse(expiryString)
            val expiryTime = date?.time ?: 0L
            val currentTime = System.currentTimeMillis()

            // Return duration remaining
            maxOf(expiryTime - currentTime, 0L)
        } catch (e: Exception) {
            300000L // Fallback to 5 mins
        }
    }



    private fun buildRequestBodyForDeleteCode(attendance: AttendanceCodeModel): RequestBody {

        return JSONObject().apply {

            put("teacherId", attendance.teacherId)
            put("department", attendance.department)
            put("subject", attendance.subject)
            put("className", attendance.className)

        }.toString().toRequestBody("application/json".toMediaTypeOrNull())

    }

    private fun buildRequestBodyForCurrentClass(teacherId: String): RequestBody {
        val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(java.util.Date())
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date())

        return JSONObject().apply {

            put("teacher_id", teacherId)
            put("day", currentDay)
            put("time", currentTime)

        }.toString().toRequestBody("application/json".toMediaTypeOrNull())

    }

    private fun formatSemester(sem: Int): String = "${sem}${getSuffix(sem)}"
    private fun getSuffix(n: Int) = when(n) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }


}