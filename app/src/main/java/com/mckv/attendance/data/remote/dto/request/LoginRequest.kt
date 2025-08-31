package com.mckv.attendance.data.remote.dto.request

data class LoginRequest(
    val email: String,
    val password: String,
//    val expectedRole: String

    val role:String
)
