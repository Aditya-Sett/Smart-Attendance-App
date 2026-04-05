package com.mckv.attendance.data.model

data class UserDetails(
    val userId: String ?=null,
    val username: String ?= null,
    val department: String ?= null,
    val email: String ?= null,
    val contact: String ?= null,
    val role: List<String> = emptyList(),
    val studentProfile: StudentProfile ?= null
)

data class StudentProfile(
    val studentId: String?= null,
    val admissionYear: String?= null,
    val collegeRoll: String?= null,
    val academicYear: String?= null,
    val semester: String?= null
)