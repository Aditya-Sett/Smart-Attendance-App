package com.mckv.attendance.model

import com.google.gson.annotations.SerializedName

data class Schedule(
    val department: String,
    val day: String,
    val time: String,
    val subject: String,
    val room: String,
    /*@SerializedName("Group")*/ val group: String
)
