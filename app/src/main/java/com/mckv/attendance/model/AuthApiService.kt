package com.mckv.attendance.model

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/auth/login")
    fun loginUser(@Body loginRequest: LoginRequest): Call<ResponseBody>

    @GET("api/auth/profile")
    fun getProfile(@Header("Authorization") token: String): Call<ResponseBody>
}