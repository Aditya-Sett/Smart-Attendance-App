package com.mckv.attendance.ui.screens.take_attendance

sealed class AttendanceUiState {
    object Idle: AttendanceUiState()
    object Loading: AttendanceUiState()
    data class FetchCurrentClassSuccess(val department: String, val subject: String, val semester: String): AttendanceUiState()
    object LoadingGenerateAttendance: AttendanceUiState()
    object ActiveSessionState: AttendanceUiState()
    object LoadingDeleteCode: AttendanceUiState()
}