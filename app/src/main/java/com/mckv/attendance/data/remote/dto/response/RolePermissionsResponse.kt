package com.mckv.attendance.data.remote.dto.response

data class RolePermissionsResponse(
    val success: Boolean,
    val message: String,
    val data: List<Permission>?,
    val timeStamp: String
)

data class Permission(
    val permission: String,
    val description: String
)
