package com.mckv.attendance.ui.screens.student

data class StudentUiState(
    val studentId: String = "",
    val department: String = "",
    val academicYear: String = "",
    val sem: String = "",
    val admissionYear: String = "",

    // Bluetooth
    val bluetoothEnabled: Boolean = false,
    val bluetoothChecking: Boolean = true,
    val bluetoothSupported: Boolean = true,

    // Snackbar
    val snackbarMessage: String? = null
)