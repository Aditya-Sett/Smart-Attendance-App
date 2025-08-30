package com.mckv.attendance.model

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @GET("api/schedule/{department}")
    suspend fun getScheduleByDepartment(@Path("department") department: String): List<Schedule>

    @POST("api/schedule/")
    suspend fun addSchedule(@Body schedule: Schedule): retrofit2.Response<Schedule>

//    @POST("api/auth/login")
//    fun loginUser(@Body loginRequest: LoginRequest): Call<ResponseBody>

    @POST("api/attendance/generate")
    fun generateCode(@Body requestBody: RequestBody): Call<ResponseBody>

    @GET("api/attendance/latest/{department}")
    fun getLatestCode(
        @Path("department") department: String
    ): Call<ResponseBody>

    @POST("api/attendance/submit")
    fun submitAttendanceCode(
        @Body requestBody: RequestBody
    ): Call<ResponseBody>

    @GET("api/attendance/summary/{studentId}/{department}")
    fun getAttendanceSummary(
        @Path("studentId") studentId: String,
        @Path("department") department: String
    ): Call<ResponseBody>

    @GET("api/attendance/students/{department}/{subject}")
    fun getStudentsAttendanceSummary(
        @Path("department") department: String,
        @Path("subject") subject: String
    ): Call<ResponseBody>

    @POST("/api/attendance/approve-leave")
    fun approveLeave(@Body body: RequestBody): Call<ResponseBody>

    @GET("api/students/{department}")
    fun getStudentsByDepartment(
        @Path("department") department: String
    ): Call<ResponseBody>
}