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
    fun verifyPasswordEmail(@Query("email") email: String,
                            @Query("otp") otp: String): Call<ResponseBody>
}