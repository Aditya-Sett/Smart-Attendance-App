package com.mckv.attendance.data.remote.dto.response

import com.mckv.attendance.data.remote.dto.dto_utils.Coordinate

data class ClassroomResponse(
    val _id: String,
    val number: String,
    val coordinates: List<Coordinate>
)
