package com.mckv.attendance.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object PermissionManager {

    private lateinit var preferences: SharedPreferences

    private const val PREF_NAME = "proxino_permission_prefs"
    private const val KEY_PERMISSIONS = "user_permissions"

    fun init(context: Context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    //SET PERMISSION
    fun setPermissions(permissions: List<String>) {
        preferences.edit().putStringSet(KEY_PERMISSIONS, permissions.toSet()).apply()
    }

    fun getPermissions(): List<String> {
        return preferences.getStringSet(KEY_PERMISSIONS, emptySet())?.toList() ?: emptyList()
    }

    fun hasPermission(permission: String): Boolean {
        return preferences
            .getStringSet(KEY_PERMISSIONS, emptySet())
            ?.contains(permission) == true
    }

    fun clearPermissions() {
        preferences.edit().clear().apply()
    }
}