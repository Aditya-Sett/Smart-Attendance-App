package com.mckv.attendance.utils

import android.content.Context
import android.provider.Settings

object DeveloperOptionsChecker {
    fun isDeveloperModeEnabled(context: Context): Boolean {
        return Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        ) != 0

//        return false
    }
}