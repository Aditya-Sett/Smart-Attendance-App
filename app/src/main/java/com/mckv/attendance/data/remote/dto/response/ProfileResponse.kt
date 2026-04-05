package com.mckv.attendance.data.remote.dto.response

import com.mckv.attendance.data.model.UserDetails

data class ProfileResponse(
    val success: Boolean,
    val message: String,
    val data: UserDetails?,
    val timeStamp: String
)
