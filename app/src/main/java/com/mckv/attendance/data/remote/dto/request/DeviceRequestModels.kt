/*
Works for Device related request and status checking
*/
package com.mckv.attendance.data.remote.dto.request

// Models for GET /api/auth/device-request-pending
data class DeviceRequestResponse(
    val success: Boolean,
    val message: String,
    val data: DeviceRequestData?
)

data class DeviceRequestData(
    val content: List<DeviceRequestItem>,
    val pageable: PageableInfo?
)

data class DeviceRequestItem(
    val userId: String,
    val username: String,
    val department: String,
    val academicYear: String,
    val semester: String,
    val rollNo: String,
    val currentDeviceHash: String?,
    val boundAt: String?,
    val lastSeenAt: String?
)

data class PageableInfo(
    val pageNumber: Int,
    val pageSize: Int
)

// Request model for Approve / Reject endpoints
data class DeviceActionRequestBody(
    val studentUserId: String
)

// Response model for Approve / Reject
data class GenericApiResponse(
    val success: Boolean,
    val message: String,
    val timeStamp: String?
)

data class DeviceStatusRequest(
    val email: String,
    val password: String
)

data class BaseResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?,
    val timeStamp: String?
)