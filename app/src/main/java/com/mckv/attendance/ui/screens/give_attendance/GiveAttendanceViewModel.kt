package com.mckv.attendance.ui.screens.give_attendance

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mckv.attendance.data.local.AttendanceManager
import com.mckv.attendance.data.local.SessionManager
import com.mckv.attendance.data.remote.RetrofitClient
import com.mckv.attendance.utils.convertUTCToISTMillis
import com.mckv.attendance.utils.getCurrentISTMillis
import com.mckv.attendance.utils.getWifiFingerPrint
import com.mckv.attendance.utils.scanForTeacherUuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class GiveAttendanceViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GiveAttendanceUiState())
    val uiState: StateFlow<GiveAttendanceUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var timerJob: Job? = null
    private var bleJob: Job? = null

    // ── BLE latch: once true within a session, never flips back to false ───────
    private var _bleLatched = false

    private val studentId get() = SessionManager.userDetails?.userId ?: ""
    private val department get() = SessionManager.userDetails?.department ?: ""
    private val academicYear get() = SessionManager.userDetails?.studentProfile?.academicYear ?: ""
    private val sem get() = SessionManager.userDetails?.studentProfile?.semester ?: ""
    private val admissionYear get() = SessionManager.userDetails?.studentProfile?.admissionYear ?: ""

    // ─── Telephony & Audio Listener References for Cleanup ───────────────────
    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallback: TelephonyCallback? = null
    private var phoneStateListener: PhoneStateListener? = null

    private var audioManager: AudioManager? = null
    private var audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    // ─── Call State & VoIP Monitoring ────────────────────────────────────────

    /**
     * Checks if the device is currently in ANY call (Cellular OR VoIP like WhatsApp/Meet/Telegram).
     */
    private fun isAnyCallActive(context: Context): Boolean {
        val appContext = context.applicationContext

        // 1. Check Cellular Call via TelephonyManager
        val tm = appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val isCellularCall = try {
            tm?.callState != TelephonyManager.CALL_STATE_IDLE
        } catch (e: SecurityException) {
            false
        }

        // 2. Check VoIP Call via AudioManager Mode (WhatsApp, Telegram, Meet use IN_COMMUNICATION)
        val am = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val audioMode = am?.mode ?: AudioManager.MODE_NORMAL
        val isVoipCall = audioMode == AudioManager.MODE_IN_COMMUNICATION ||
                audioMode == AudioManager.MODE_IN_CALL

        return isCellularCall || isVoipCall
    }

    fun monitorCallState(context: Context) {
        val appContext = context.applicationContext

        // 1. Verify runtime permission first
        val hasPermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            _uiState.update { it.copy(isCallPermissionGranted = false, isOnCall = false) }
            return
        }

        _uiState.update { it.copy(isCallPermissionGranted = true) }

        // 2. Immediately evaluate current status (Cellular + VoIP)
        val currentCallState = isAnyCallActive(appContext)
        _uiState.update { it.copy(isOnCall = currentCallState) }

        // 3. Register Telephony Listener (For SIM/Cellular Calls)
        if (telephonyManager == null) {
            val tm = appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                telephonyManager = tm
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    registerTelephonyCallbackApi31(appContext, tm)
                } else {
                    registerPhoneStateListenerLegacy(appContext, tm)
                }
            }
        }

        // 4. Register AudioFocus Listener (For VoIP Calls: WhatsApp, Telegram, Meet)
        if (audioManager == null) {
            val am = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (am != null) {
                audioManager = am
                registerAudioFocusListener(appContext, am)
            }
        }
    }

    private fun registerAudioFocusListener(context: Context, am: AudioManager) {
        audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { _ ->
            // Audio focus changed (e.g. WhatsApp started/ended a call)
            val updatedCallState = isAnyCallActive(context)
            _uiState.update { it.copy(isOnCall = updatedCallState) }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(playbackAttributes)
                .setOnAudioFocusChangeListener(audioFocusChangeListener!!)
                .build()

            audioFocusRequest = request
            am.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerTelephonyCallbackApi31(context: Context, tm: TelephonyManager) {
        val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                val updatedCallState = isAnyCallActive(context)
                _uiState.update { it.copy(isOnCall = updatedCallState) }
            }
        }
        telephonyCallback = callback
        tm.registerTelephonyCallback(ContextCompat.getMainExecutor(context), callback)
    }

    @Suppress("DEPRECATION")
    private fun registerPhoneStateListenerLegacy(context: Context,tm: TelephonyManager) {
        val listener = object : PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onCallStateChanged(state: Int, incomingNumber: String?) {
                // Re-evaluate both Cellular and VoIP
                val updatedCallState = isAnyCallActive(context)
                _uiState.update { it.copy(isOnCall = updatedCallState) }
            }
        }
        phoneStateListener = listener
        tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun unregisterCallMonitoring() {
        // Unregister Telephony
        telephonyManager?.let { tm ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyCallback?.let { callback ->
                    tm.unregisterTelephonyCallback(callback)
                }
            } else {
                phoneStateListener?.let { listener ->
                    @Suppress("DEPRECATION")
                    tm.listen(listener, PhoneStateListener.LISTEN_NONE)
                }
            }
        }
        telephonyCallback = null
        phoneStateListener = null
        telephonyManager = null

        // Abandon Audio Focus
        audioManager?.let { am ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { request -> am.abandonAudioFocusRequest(request) }
            } else {
                audioFocusChangeListener?.let { listener ->
                    @Suppress("DEPRECATION")
                    am.abandonAudioFocus(listener)
                }
            }
        }
        audioFocusRequest = null
        audioFocusChangeListener = null
        audioManager = null
    }

    // ─── Public API ────────────────────────────────────────────────────────────

    fun startPollingAndBle(context: Context) {
        _bleLatched = false   // fresh session — reset latch
        monitorCallState(context)
        startCodePolling(context)
    }

    fun stopAll() {
        pollingJob?.cancel()
        timerJob?.cancel()
        bleJob?.cancel()
        _bleLatched = false   // reset latch so next visit starts clean
        unregisterCallMonitoring()
    }

    fun onInputCodeChanged(code: String) {
        if (code.length <= 4) _uiState.update { it.copy(inputCode = code) }
    }

    fun submitAttendance(context: Context) {
        val state = _uiState.value
        if (state.inputCode.length != 4 || state.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            try {
                doSubmit(context, state)
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun clearSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }

    fun clearSubmissionResult() = _uiState.update { it.copy(submissionResult = null) }

    fun resetExpiry() {
        _bleLatched = false   // reset latch on expiry/dismiss
        _uiState.update {
            it.copy(
                isExpired = false,
                timeLeftMillis = null,
                activeCode = null,
                activeSubject = null,
                expiresAtMillis = null,
                isCodeAvailable = false,
                isTeacherNearby = false,
                inputCode = ""
            )
        }
        timerJob?.cancel()
    }

    // ─── Code Polling ──────────────────────────────────────────────────────────

    private fun startCodePolling(context: Context) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            _uiState.update { it.copy(isPollingCode = true) }
            while (isActive) {
                // Re-verify call status on every poll cycle as an extra safeguard
                val activeCall = isAnyCallActive(context)
                if (_uiState.value.isOnCall != activeCall) {
                    _uiState.update { it.copy(isOnCall = activeCall) }
                }

                fetchLatestCode(context)
                delay(2_000L)
            }
        }
    }

    private suspend fun fetchLatestCode(context: Context) {
        val json = JSONObject().apply {
            put("department", department)
            put("admissionYear", admissionYear)
        }
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

        try {
            val response = RetrofitClient.instance.getLatestCode2(body)
            if (response.isSuccessful) {
                fetchLatestCodeRaw(context, body)
            } else {
                _uiState.update { it.copy(isCodeAvailable = false) }
            }
        } catch (e: Exception) {
            Log.e("GiveAttendanceVM", "Polling error: ${e.message}")
            _uiState.update { it.copy(isCodeAvailable = false) }
        }
    }

    private fun fetchLatestCodeRaw(context: Context, body: okhttp3.RequestBody) {
        RetrofitClient.instance.getLatestCode(body).enqueue(object : retrofit2.Callback<okhttp3.ResponseBody> {
            override fun onResponse(
                call: retrofit2.Call<okhttp3.ResponseBody>,
                response: retrofit2.Response<okhttp3.ResponseBody>
            ) {
                if (!response.isSuccessful) {
                    _uiState.update { it.copy(isCodeAvailable = false) }
                    return
                }
                val bodyStr = response.body()?.string() ?: return
                val json = JSONObject(bodyStr)

                val code = json.optString("code")
                val subject = json.optString("subject")
                val expiresAtUTC = json.optString("expiresAt")
                val bluetoothUuid = json.optString("bluetoothUuid")
                val expiresAtIST = convertUTCToISTMillis(expiresAtUTC)

                if (code.isBlank() || expiresAtIST <= 0) {
                    _uiState.update { it.copy(isCodeAvailable = false) }
                    return
                }

                val alreadySubmitted = AttendanceManager.lastCodeSubmitted == code
                if (alreadySubmitted) {
                    _uiState.update { it.copy(isCodeAvailable = false) }
                    return
                }

                _uiState.update {
                    it.copy(
                        isCodeAvailable = true,
                        activeCode = code,
                        activeSubject = subject,
                        expiresAtMillis = expiresAtIST,
                        bluetoothUuid = bluetoothUuid
                    )
                }

                startTimerIfNeeded(expiresAtIST)
                startBleScanning(context, bluetoothUuid)
            }

            override fun onFailure(call: retrofit2.Call<okhttp3.ResponseBody>, t: Throwable) {
                Log.e("GiveAttendanceVM", "Network: ${t.message}")
                _uiState.update { it.copy(isCodeAvailable = false) }
            }
        })
    }

    // ─── BLE Scanning ──────────────────────────────────────────────────────────

    private fun startBleScanning(context: Context, bluetoothUuid: String?) {
        if (bluetoothUuid.isNullOrBlank()) return
        if (_bleLatched) return

        bleJob?.cancel()
        bleJob = viewModelScope.launch {
            val isTeacherFound = scanForTeacherUuid(context, bluetoothUuid)

            if (isTeacherFound && !_bleLatched) {
                Log.d("BLE", "✅ Teacher nearby. 🔒 BLE latched for this session")
                _bleLatched = true
                _uiState.update { it.copy(isTeacherNearby = true) }
            }
        }
    }

    // ─── Timer ─────────────────────────────────────────────────────────────────

    private fun startTimerIfNeeded(expiresAtIST: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val now = getCurrentISTMillis()
                val left = expiresAtIST - now
                if (left <= 0) {
                    _uiState.update {
                        it.copy(
                            timeLeftMillis = 0,
                            isExpired = true,
                            isCodeAvailable = false,
                            isTeacherNearby = false
                        )
                    }
                    _bleLatched = false   // session ended via expiry — reset latch
                    bleJob?.cancel()     // stop BLE scan when code expires
                    break
                }
                _uiState.update { it.copy(timeLeftMillis = left) }
                delay(1_000L)
            }
        }
    }

    // ─── Submit ────────────────────────────────────────────────────────────────

    private suspend fun doSubmit(context: Context, state: GiveAttendanceUiState) {
        val wifiFingerprint = getWifiFingerPrint(context)

        val parts = academicYear.split("-")
        val shortYear = if (parts.size >= 2) "${parts[0]}-${parts[1].takeLast(2)}" else academicYear
        val formattedSem = mapSemester(sem)

        val json = JSONObject().apply {
            put("studentId", studentId)
            put("department", department)
            put("code", state.inputCode)
            put("academic_year", shortYear)
            put("sem", formattedSem)
            put("wifiFingerprint", wifiFingerprint)
        }
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

        try {
            val result = kotlinx.coroutines.suspendCancellableCoroutine<SubmissionResult> { cont ->
                RetrofitClient.instance.submitAttendanceCode(body)
                    .enqueue(object : retrofit2.Callback<okhttp3.ResponseBody> {
                        override fun onResponse(
                            call: retrofit2.Call<okhttp3.ResponseBody>,
                            response: retrofit2.Response<okhttp3.ResponseBody>
                        ) {
                            if (response.isSuccessful) {
                                val r = JSONObject(response.body()?.string() ?: "{}")
                                if (r.optBoolean("success")) {
                                    AttendanceManager.lastCodeSubmitted = state.activeCode
                                    cont.resume(SubmissionResult.Success) {}
                                } else {
                                    cont.resume(SubmissionResult.Failure("❌ Invalid code or WiFi mismatch")) {}
                                }
                            } else {
                                val err = response.errorBody()?.string() ?: "Unknown error"
                                cont.resume(SubmissionResult.Failure("⚠️ $err")) {}
                            }
                        }

                        override fun onFailure(call: retrofit2.Call<okhttp3.ResponseBody>, t: Throwable) {
                            cont.resume(SubmissionResult.Failure("🚫 Network error: ${t.message}")) {}
                        }
                    })
            }

            if (result is SubmissionResult.Success) {
                stopAll()   // stopAll() also resets _bleLatched
                _uiState.update {
                    it.copy(
                        submissionResult = result,
                        snackbarMessage = "✅ Attendance marked successfully",
                        inputCode = "",
                        isCodeAvailable = false,
                        isTeacherNearby = false
                    )
                }
            } else {
                _uiState.update { it.copy(submissionResult = result) }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(submissionResult = SubmissionResult.Failure("🚫 ${e.message}"))
            }
        }
    }

    private fun mapSemester(sem: String) = when (sem) {
        "1" -> "1st"; "2" -> "2nd"; "3" -> "3rd"; "4" -> "4th"
        "5" -> "5th"; "6" -> "6th"; "7" -> "7th"; "8" -> "8th"
        else -> sem
    }

    override fun onCleared() {
        super.onCleared()
        stopAll()
    }
}