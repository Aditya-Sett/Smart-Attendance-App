package com.mckv.attendance.data.remote.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AnalysisService {
    @GET("api/reports/department/{department}")
    suspend fun getDepartmentReport(
        @Path("department") department: String
    ): Response<ResponseBody>

    @POST("api/teacher/current-class")
    fun getCurrentClass(
        @Body body: RequestBody
    ): Call<ResponseBody>

    @POST("api/teacher/current-class")
    suspend fun getCurrentClass2( @Body body: RequestBody): retrofit2.Response<ClassDataModel>

    @POST("api/attendancecodes/by-teacher-date")
    suspend fun getAttendanceSessions(
        @Body body: RequestBody
    ): Response<ResponseBody>

    @POST("api/attendance/get-attendance-summary")
    suspend fun getAttendanceSummary(
        @Body body: RequestBody
    ): Response<ResponseBody>

    @POST("api/attendance/report")
    suspend fun getReport(
        @Body request: RequestBody
    ): Response<ResponseBody>

}

data class ClassDataModel(
    val department: String,
    val subject: String,
    val semester: Int,
    val success: Boolean
)