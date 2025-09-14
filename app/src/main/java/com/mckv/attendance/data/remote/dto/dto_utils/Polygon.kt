package com.mckv.attendance.data.remote.dto.dto_utils

data class Polygon(
    val type: String = "Polygon",
    val coordinates: List<List<List<Double>>>
)
