package com.mckv.attendance.ui.screens.give_attendance

data class GiveAttendanceUiState(
    // Status bulbs
    val isCodeAvailable: Boolean = false,       // Bulb 1: green = active code found from server
    val isTeacherNearby: Boolean = false,        // Bulb 2: green = BLE UUID matched

    // Code polling
    val isPollingCode: Boolean = false,
    val activeCode: String? = null,
    val activeSubject: String? = null,
    val expiresAtMillis: Long? = null,
    val bluetoothUuid: String? = null,

    // BLE scan
    val isScanningBle: Boolean = false,

    // Timer
    val timeLeftMillis: Long? = null,
    val isExpired: Boolean = false,

    // Form input
    val inputCode: String = "",

    // Submission
    val isSubmitting: Boolean = false,
    val submissionResult: SubmissionResult? = null,

    // Snackbar
    val snackbarMessage: String? = null
)

sealed class SubmissionResult {
    object Success : SubmissionResult()
    data class Failure(val message: String) : SubmissionResult()
}