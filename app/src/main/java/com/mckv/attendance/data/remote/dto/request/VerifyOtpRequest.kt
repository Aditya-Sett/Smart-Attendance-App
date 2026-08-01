/*
Create a request body model for the OTP verification step
*/
package com.mckv.attendance.data.remote.dto.request

data class VerifyOtpRequest(
    val email: String,
    val otp: String
)
