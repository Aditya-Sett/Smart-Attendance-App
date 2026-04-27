package com.mckv.attendance.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.mckv.attendance.data.remote.api.AttendanceCodeModel
import com.mckv.attendance.utils.getCurrentISTMillis
import java.text.SimpleDateFormat
import java.util.Locale

object ActiveCodeManager {
    private const val PREF_NAME = "active_attendance_prefs"
    private const val KEY_ACTIVE_CODE = "active_code_json"

    // Helper to get preferences safely
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // Save the generated code
    fun saveActiveCode(context: Context, model: AttendanceCodeModel) {
        val json = Gson().toJson(model)
        getPrefs(context).edit {
            putString(KEY_ACTIVE_CODE, json)
            apply() // Use apply() for asynchronous background saving
        }
    }

    // Retrieve and validate expiry
    fun getActiveCode(context: Context): AttendanceCodeModel? {
        val prefs = getPrefs(context)
        val json = prefs.getString(KEY_ACTIVE_CODE, null) ?: return null

        return try {
            val model = Gson().fromJson(json, AttendanceCodeModel::class.java)

            // Check Expiry Logic
            val format = SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.getDefault())
            val expTime = format.parse(model.expiresAt)?.time ?: 0L
            val currentIST = getCurrentISTMillis()

            if (currentIST < expTime) {
                model
            } else {
                // IMPORTANT: Use the local prefs reference to clear
                prefs.edit { remove(KEY_ACTIVE_CODE) }
                null
            }
        } catch (e: Exception) {
            // If JSON is malformed or Date parsing fails, clear it
            prefs.edit { remove(KEY_ACTIVE_CODE) }
            null
        }
    }

    fun clearActiveCode(context: Context) {
        getPrefs(context).edit {
            remove(KEY_ACTIVE_CODE)
            apply()
        }
    }
}