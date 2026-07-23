package com.mckv.attendance.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat

fun getCurrentAppVersionCode(context: Context): Int {
    return try {
        // Get package info for your app package ("com.mckv.attendance")
        val pInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

        // Securely read the versionCode as a Long and cast to Int
        PackageInfoCompat.getLongVersionCode(pInfo).toInt()
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        1 // Fallback to 1 if something goes wrong
    }
}