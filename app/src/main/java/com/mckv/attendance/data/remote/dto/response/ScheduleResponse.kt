package com.mckv.attendance.data.remote.dto.response

data class ScheduleResponse(
    val department: String,
    val day: String,
    val time: String,
    val subject: String,
    val room: String,
    /*@SerializedName("Group")*/ val group: String
)
