package com.mckv.attendance.data.remote.api

import com.mckv.attendance.data.remote.dto.request.BaseResponse
import com.mckv.attendance.data.remote.dto.request.ChangePasswordRequest
import com.mckv.attendance.data.remote.dto.request.DeviceActionRequestBody
import com.mckv.attendance.data.remote.dto.request.DeviceRequestResponse
import com.mckv.attendance.data.remote.dto.request.DeviceStatusRequest
import com.mckv.attendance.data.remote.dto.request.GenericApiResponse
import com.mckv.attendance.data.remote.dto.request.LoginRequest
import com.mckv.attendance.data.remote.dto.request.VerifyOtpRequest
import com.mckv.attendance.data.remote.dto.response.ApiResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface AuthApiService {

    @POST("api/auth/login")
    fun loginUser(@Body loginRequest: LoginRequest): Call<ResponseBody>

    @GET("api/auth/profile")
    fun getProfile(@Header("Authorization") token: String): Call<ResponseBody>

    @PUT("api/auth/change-password")
    fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Call<ApiResponse<Map<String, Boolean>?>>

    // 🔹 SEND VERIFICATION CODE (GET)
    @GET("api/auth/verify-email-code")
    fun sendEmailVerificationCode(
        @Query("email") email: String
    ): Call<ResponseBody>


    // 🔹 VERIFY CODE (POST)
    @POST("api/auth/verify-email-code")
    fun verifyEmailCode(
        @Body body: Map<String, String>
    ): Call<ResponseBody>


    /*// 🔹 REGISTER STUDENT
    @POST("api/auth/register-student")
    fun registerStudent(
        @Body body: Map<String, Any>
    ): Call<ResponseBody>


    // 🔹 REGISTER FACULTY
    @POST("api/auth/register-faculty")
    fun registerFaculty(
        @Body body: Map<String, Any>
    ): Call<ResponseBody>

     */

    @POST("api/auth/register-student")
    fun registerStudent(@Body request: Map<String, String>): Call<ResponseBody>

    @POST("api/auth/register-faculty")
    fun registerFaculty(@Body request: Map<String, String>): Call<ResponseBody>

    // GET Request to send OTP to email
    @GET("api/auth/forgot-password-email")
    fun sendForgotPasswordOtp(
        @Query("email") email: String
    ): Call<ApiResponse<String?>>

    // POST Request to verify OTP
    @POST("api/auth/verify-password-email")
    fun verifyPasswordOtp(
        @Body request: VerifyOtpRequest
    ): Call<ApiResponse<String?>>

    @POST("api/auth/device-request-status")
    fun checkDeviceRequestStatus(@Body request: DeviceStatusRequest): Call<BaseResponse<String>>

    @POST("api/auth/device-request-change")
    fun requestDeviceChange(@Body request: DeviceStatusRequest): Call<BaseResponse<Nothing>>

    @GET("api/auth/device-request-pending")
    suspend fun getPendingDeviceRequests(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<DeviceRequestResponse>

    @POST("api/auth/device-request-approve")
    suspend fun approveDeviceRequest(
        @Header("Authorization") token: String,
        @Body body: DeviceActionRequestBody
    ): Response<GenericApiResponse>

    @POST("api/auth/device-request-reject")
    suspend fun rejectDeviceRequest(
        @Header("Authorization") token: String,
        @Body body: DeviceActionRequestBody
    ): Response<GenericApiResponse>
}