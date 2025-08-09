package com.mckv.attendance.model

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null
)
