package com.mckv.attendance.data.remote.dto.request

import com.mckv.attendance.data.remote.dto.dto_utils.Polygon

data class ClassroomRequest(
    val number: String,
    val polygon: Polygon
)
