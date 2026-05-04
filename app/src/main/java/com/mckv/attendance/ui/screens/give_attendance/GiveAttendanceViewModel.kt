package com.mckv.attendance.ui.screens.give_attendance

import android.content.Context
import android.util.Log
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

    // ─── Public API ────────────────────────────────────────────────────────────

    fun startPollingAndBle(context: Context) {
        _bleLatched = false   // fresh session — reset latch
        startCodePolling(context)
    }

    fun stopAll() {
        pollingJob?.cancel()
        timerJob?.cancel()
        bleJob?.cancel()
        _bleLatched = false   // reset latch so next visit starts clean
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

                // ✅ Bulb 1 goes green
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

        // If already latched, BLE bulb is already green — no need to restart scanning
        if (_bleLatched) return

        bleJob?.cancel()
        bleJob = viewModelScope.launch {
            while (isActive && _uiState.value.isCodeAvailable) {
                scanForTeacherUuid(context, bluetoothUuid) { matched ->
                    Log.d("BLE", if (matched) "✅ Teacher nearby" else "❌ Teacher not nearby")

                    if (matched && !_bleLatched) {
                        // First time we see the teacher — latch permanently for this session
                        _bleLatched = true
                        Log.d("BLE", "🔒 BLE latched — will stay green for this session")
                    }

                    // Always reflect the latched state, never go back to false
                    _uiState.update { it.copy(isTeacherNearby = _bleLatched) }
                }

                // Once latched, no point continuing to scan — cancel this job
                if (_bleLatched) {
                    Log.d("BLE", "🛑 BLE scan stopped — already latched")
                    break
                }

                delay(3_000L)
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