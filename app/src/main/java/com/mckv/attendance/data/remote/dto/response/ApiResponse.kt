/*
standard API response wrapper
*/
package com.mckv.attendance.data.remote.dto.response

data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?,
    val timeStamp: String?
)
