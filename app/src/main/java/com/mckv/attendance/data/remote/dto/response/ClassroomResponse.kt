package com.mckv.attendance.data.remote.dto.response

import com.mckv.attendance.data.remote.dto.dto_utils.Polygon

data class ClassroomResponse(
    val _id: String,
    val number: String,
    val polygon: Polygon
)
