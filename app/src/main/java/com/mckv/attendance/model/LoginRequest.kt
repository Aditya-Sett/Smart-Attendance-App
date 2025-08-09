package com.mckv.attendance.model

data class LoginRequest(
    val email: String,
    val password: String,
    val expectedRole: String
)
