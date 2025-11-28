package com.mckv.attendance.data.local

import android.content.Context
import android.content.SharedPreferences
import com.mckv.attendance.utils.JwtUtils

object SessionManager {
    private lateinit var preferences: SharedPreferences
    private const val PREF_NAME = "attendance_pref"

    fun init(context: Context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    var authToken: String?
        get()= preferences.getString("authToken",null)
        set(value)= preferences.edit().putString("authToken",value).apply()

    val isLoggedIn: Boolean
        get() = preferences.getBoolean("isLoggedIn", false) &&
                !isTokenExpired() &&
                !authToken.isNullOrEmpty()

    // Add login state tracking
//    var isLoggedIn: Boolean
//        get() = preferences.getBoolean("isLoggedIn", false)
//        set(value) = preferences.edit().putBoolean("isLoggedIn", value).apply()

    // Add user role tracking
    var userRole: String?
        get() = preferences.getString("userRole", null)
        set(value) = preferences.edit().putString("userRole", value).apply()

    // Add user ID tracking (common for all roles)
    var userId: String?
        get() = preferences.getString("userId", null)
        set(value) = preferences.edit().putString("userId", value).apply()


    // Admin
    var adminId: String?
        get() = preferences.getString("adminId", null)
        set(value) = preferences.edit().putString("adminId", value).apply()

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

    var admissionYear: String?
        get() = preferences.getString("admissionYear", null)
        set(value) = preferences.edit().putString("admissionYear", value).apply()


    // Attendance Code Submission Tracking
    var lastCodeSubmitted: String?
        get() = preferences.getString("lastCodeSubmitted", null)
        set(value) = preferences.edit().putString("lastCodeSubmitted", value).apply()

    // Enhanced clear function that properly logs out
    fun clear() {
        val wasLoggedIn = preferences.getBoolean("isLoggedIn", false)
        preferences.edit().clear().apply()
        System.out.println("🔓 Session cleared. Was logged in: $wasLoggedIn")
    }

    // Helper function to save complete login session
    fun saveLoginSession(token: String, role: String, id: String) {

        val expiryTime = JwtUtils.getTokenExpiryTime(token)

        preferences.edit().apply {
            putString("authToken", token)
            putString("userRole", role)
            putString("userId", id)
            putBoolean("isLoggedIn", true)
            putLong("tokenExpiryTime", expiryTime * 1000) // Convert to milliseconds
            apply()
        }

        System.out.println("💾 LOGIN SESSION SAVED:")
        System.out.println("   - Role: $role")
        System.out.println("   - User ID: $id")
        System.out.println("   - Token: ${token.take(10)}...")
        System.out.println("   - isLoggedIn: true")
        System.out.println("Expiry time: ${JwtUtils.getTokenExpiryDate(token)}")
    }

    // Check if user should be automatically logged in
    fun shouldAutoLogin(): Boolean {
        return isLoggedIn && !authToken.isNullOrEmpty()
    }

    // Enhanced token expiration check
    fun isTokenExpired(): Boolean {
        val token = authToken
        return if (!token.isNullOrEmpty()) {
            JwtUtils.isTokenExpired(token)
        } else {
            true
        }
    }

    fun printSessionStatus() {
        val token = authToken
        System.out.println("🔍 SESSION STATUS:")
        System.out.println("   - isLoggedIn: $isLoggedIn")
        System.out.println("   - userRole: $userRole")
        System.out.println("   - authToken: ${token?.take(10)}...")
        System.out.println("   - tokenExpired: ${isTokenExpired()}")

        if (!token.isNullOrEmpty()) {
            System.out.println("   - expiresAt: ${JwtUtils.getTokenExpiryDate(token)}")
            System.out.println("   - timeUntilExpiry: ${JwtUtils.getTimeUntilExpiry(token)}")
        }

        System.out.println("   - studentId: $studentId")
        System.out.println("   - teacherId: $teacherId")
        System.out.println("   - adminId: $adminId")
    }
}
