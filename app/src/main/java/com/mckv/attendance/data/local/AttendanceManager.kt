package com.mckv.attendance.data.local

import android.content.Context
import android.content.SharedPreferences

object AttendanceManager {
    private lateinit var preferences: SharedPreferences
    private const val PREF_NAME = "attendance_pref"

    fun init(context: Context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // Attendance Code Submission Tracking
    var lastCodeSubmitted: String?
        get() = preferences.getString("lastCodeSubmitted", null)
        set(value) = preferences.edit().putString("lastCodeSubmitted", value).apply()


}
