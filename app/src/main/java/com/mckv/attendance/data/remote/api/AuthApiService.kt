package com.mckv.attendance.data.remote.api

import com.mckv.attendance.data.remote.dto.request.LoginRequest
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApiService {

    @POST("api/auth/login")
    fun loginUser(@Body loginRequest: LoginRequest): Call<ResponseBody>

    @GET("api/auth/profile")
    fun getProfile(@Header("Authorization") token: String): Call<ResponseBody>

    @POST("api/auth/forgot-password-email")
    fun forgotPassword(@Query("email") email: String): Call<ResponseBody>

    @POST("api/auth/verify-password-email")
    fun verifyPasswordEmail(
        @Query("email") email: String,
        @Query("otp") otp: String
    ): Call<ResponseBody>

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
}