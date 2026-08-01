/*
Change password request body
*/
package com.mckv.attendance.data.remote.dto.request

data class ChangePasswordRequest(
    val password: String,
    val newPassword: String
)
