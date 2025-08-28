package com.mckv.attendance.model

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.mckv.attendance.BuildConfig


object RetrofitClient {
    private const val BASE_URL = BuildConfig.BASE_URL
    private const val BASE_AUTH_URL=BuildConfig.BASE_AUTH_URL;

    // 1. Create a logging interceptor
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        System.out.println("📡 RetrofitLog: $message")
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // 2. Attach the logging interceptor to OkHttpClient
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
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
    val instance: ApiService by lazy {
        createRetrofit(BASE_URL).create(ApiService::class.java)
    }

    // Auth API service - corrected to use AuthApiService interface
    val authInstance: AuthApiService by lazy {
        createRetrofit(BASE_AUTH_URL).create(AuthApiService::class.java)
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