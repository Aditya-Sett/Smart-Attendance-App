package com.mckv.attendance.model

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.mckv.attendance.BuildConfig


object RetrofitClient {
    private const val BASE_URL = /*"http://192.168.0.176:5000/"*/ BuildConfig.BASE_URL

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

    // 3. Use the custom client in Retrofit builder
    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client) // attach custom client here
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}