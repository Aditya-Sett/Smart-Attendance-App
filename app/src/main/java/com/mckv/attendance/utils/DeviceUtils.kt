package com.mckv.attendance.utils

import android.content.Context
import android.media.MediaDrm
import android.provider.Settings
import android.util.Base64
import java.util.UUID

object DeviceUtils {

    fun getDeviceId(context: Context): String {
        return try {
            // The official UUID for the Widevine DRM module across all Android devices
            val widevineUuid = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)

            val mediaDrm = MediaDrm(widevineUuid)
            val deviceIdBytes = mediaDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)

            // Always close to prevent memory leaks
            mediaDrm.close()

            // Convert the raw hardware bytes into a clean, readable string for your database
            Base64.encodeToString(deviceIdBytes, Base64.NO_WRAP)

        } catch (e: Exception) {
            // FALLBACK: Only triggers if the phone is a custom ROM without DRM chips
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_DEVICE"
        }
    }
}