package com.mckv.attendance.data.remote

import com.mckv.attendance.BuildConfig
import com.mckv.attendance.data.remote.api.AnalysisService
import com.mckv.attendance.data.remote.api.AttendanceService
import com.mckv.attendance.data.remote.api.AuthApiService
import com.mckv.attendance.data.remote.api.RolePermissionApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object RetrofitClient {
    private const val BASE_URL = BuildConfig.BASE_URL
    private const val BASE_AUTH_URL=BuildConfig.BASE_AUTH_URL;
    private const val BASE_ROLE_URL=BuildConfig.BASE_ROLE_URL;
    const val BASE_ANALYSIS_URL=BuildConfig.BASE_ANALYSIS_URL;

    // 1. Create a logging interceptor
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        System.out.println("📡 RetrofitLog: $message")
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor ( loggingInterceptor )
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)   // time to establish connection
        .readTimeout(30, TimeUnit.SECONDS)      // time to wait for response
        .writeTimeout(30, TimeUnit.SECONDS)     // time to send request
        .build()


//    // 3. Use the custom client in Retrofit builder
//    val instance: ApiService by lazy {
//        Retrofit.Builder()
//            .baseUrl(BASE_URL)
//            .client(client) // attach custom client here
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//            .create(ApiService::class.java)
//    }
//
//    // 3. Use the custom client in Retrofit builder
//    val authInstance: AuthApiService by lazy {
//        Retrofit.Builder()
//            .baseUrl(BASE_AUTH_URL)
//            .client(client) // attach custom client here
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//            .create(ApiService::class.java)
//    }

    // Main API service
    val instance: AttendanceService by lazy {
        createRetrofit(BASE_URL).create(AttendanceService::class.java)
    }

    // Auth API service - corrected to use AuthApiService interface
    val authInstance: AuthApiService by lazy {
        createRetrofit(BASE_AUTH_URL).create(AuthApiService::class.java)
    }

    // Role Permission Api Service
    val rolePermissionInstance: RolePermissionApiService by lazy {
        createRetrofit(BASE_ROLE_URL).create(RolePermissionApiService::class.java)
    }

    // Analysis Api Service
    val analysisInstance: AnalysisService by lazy {
        createRetrofit(BASE_ANALYSIS_URL).create(AnalysisService::class.java)
    }

    // Helper function to avoid code duplication
    private fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}