package com.mckv.attendance.data.remote.api

import com.mckv.attendance.data.remote.dto.request.ScheduleRequest
import com.mckv.attendance.data.remote.dto.response.ScheduleResponse
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
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

interface AnalysisService {
    @GET("api/reports/department/{department}")
    suspend fun getDepartmentReport(
        @Path("department") department: String
    ): Response<ResponseBody>

    @POST("api/teacher/current-class")
    fun getCurrentClass(
        @Body body: RequestBody
    ): Call<ResponseBody>
}