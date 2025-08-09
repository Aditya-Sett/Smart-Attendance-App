package com.mckv.attendance

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private lateinit var preferences: SharedPreferences
    private const val PREF_NAME = "attendance_pref"

    fun init(context: Context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // Teacher
    var teacherId: String?
        get() = preferences.getString("teacherId", null)
        set(value) = preferences.edit().putString("teacherId", value).apply()

    // Student
    var studentId: String?
        get() = preferences.getString("studentId", null)
        set(value) = preferences.edit().putString("studentId", value).apply()

    var department: String?
        get() = preferences.getString("department", null)
        set(value) = preferences.edit().putString("department", value).apply()

    // Attendance Code Submission Tracking
    var lastCodeSubmitted: String?
        get() = preferences.getString("lastCodeSubmitted", null)
        set(value) = preferences.edit().putString("lastCodeSubmitted", value).apply()

    fun clear() {
        preferences.edit().clear().apply()
    }
}
