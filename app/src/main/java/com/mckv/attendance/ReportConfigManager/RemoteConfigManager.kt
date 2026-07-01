package com.mckv.attendance.ReportConfigManager

import com.google.firebase.Firebase
//import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
//import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.tasks.await

object RemoteConfigManager {

    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig

    fun init() {
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()

        remoteConfig.setConfigSettingsAsync(settings)

        remoteConfig.setDefaultsAsync(
            mapOf(
                "BASE_URL" to "https://smart-attendance-backend-aoa8.onrender.com/",
                "BASE_AUTH_URL" to "https://auth-service-49302815-0bhv000.onrender.com/",
                "BASE_ROLE_URL" to "https://role-and-permission-service-49302815.onrender.com/",
                "BASE_ANALYSIS_URL" to "https://college-erp-handling.onrender.com/"
            )
        )
    }

    // ✅ Coroutine-friendly suspend version for SplashScreen
    suspend fun fetchSuspend(): Boolean {
        return try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            false
        }
    }

    fun getBaseUrl() = remoteConfig.getString("BASE_URL")
    fun getAuthUrl() = remoteConfig.getString("BASE_AUTH_URL")
    fun getRoleUrl() = remoteConfig.getString("BASE_ROLE_URL")
    fun getAnalysisUrl() = remoteConfig.getString("BASE_ANALYSIS_URL")
}