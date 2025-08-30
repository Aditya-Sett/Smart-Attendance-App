package com.mckv.attendance.data.remote.dto.response

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null
)
