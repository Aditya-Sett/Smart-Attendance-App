package com.mckv.attendance.data.remote.api

import com.mckv.attendance.data.remote.dto.request.ScheduleRequest
import com.mckv.attendance.data.remote.dto.response.ScheduleResponse
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import com.mckv.attendance.data.remote.dto.request.ClassroomRequest
import com.mckv.attendance.data.remote.dto.response.ClassroomResponse
import com.mckv.attendance.ui.screens.UploadedCurriculum
import retrofit2.http.PUT
import retrofit2.http.Query

interface ApiService {
    @GET("api/classrooms/details")
    suspend fun getClassrooms(): Response<List<ClassroomResponse>>

    @POST("api/classrooms/create")
    suspend fun addClassroom(@Body classroom: ClassroomRequest): Response<ResponseBody>

    @GET("api/schedule/{department}")
    suspend fun getScheduleByDepartment(@Path("department") department: String): List<ScheduleRequest>

    @POST("api/schedule/")
    suspend fun addSchedule(@Body schedule: ScheduleRequest): retrofit2.Response<ScheduleResponse>

//    @POST("api/auth/login")
//    fun loginUser(@Body loginRequest: LoginRequest): Call<ResponseBody>

    @POST("api/attendance/generate")
    fun generateCode(@Body requestBody: RequestBody): Call<ResponseBody>

    @POST("api/attendance/latest")
    fun getLatestCode(
        @Body body: RequestBody
    ): Call<ResponseBody>

    @POST("api/attendance/submit")
    fun submitAttendanceCode(
        @Body requestBody: RequestBody
    ): Call<ResponseBody>

    @POST("api/attendance/delete")
    fun closeCode(
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

    @GET("api/attendance/export/{department}/{subject}/{className}/{academicYear}")
    fun exportAttendanceExcel(
        @Path("department") department: String,
        @Path("subject") subject: String,
        @Path("className") className: String,
        @Path("academicYear") academicYear: String
    ): Call<ResponseBody>

    @POST("api/attendance/attendance_taken_by_teacherid")
    suspend fun attendanceTakenBySelf(
        @Body requestBody: RequestBody
    ): ResponseBody

    @POST("api/attendance/details")
    suspend fun getAttendanceDetails(@Body body: RequestBody): ResponseBody

    @POST("api/curriculum/upload")
    suspend fun uploadCurriculum(
        @Body curriculum: UploadedCurriculum
    ): Response<ResponseBody>

    @GET("api/curriculum/summary")
    suspend fun getCurriculumSummary(): Response<ResponseBody>

    @GET("api/curriculum/get")
    suspend fun getCurriculum(
        @Query("className") className: String?,
        @Query("department") department: String?,
        @Query("effectiveYear") effectiveYear: String?
    ): Response<ResponseBody>

    @GET("api/curriculum/{id}")
    suspend fun getCurriculumById(@Path("id") id: String): Response<ResponseBody>

    @PUT("api/curriculum/update/{id}")
    suspend fun updateCurriculum(
        @Path("id") id: String,
        @Body requestBody: RequestBody
    ): Response<ResponseBody>

    @POST("api/curriculum/new")
    suspend fun createCurriculum(
        @Body requestBody: RequestBody
    ): Response<ResponseBody>
}