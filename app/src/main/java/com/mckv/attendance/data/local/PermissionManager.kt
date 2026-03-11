package com.mckv.attendance.data.local

import android.util.Log

object PermissionManager {

    private val userPermissions = mutableSetOf<String>()

    fun setPermissions(permissions: List<String>) {

        userPermissions.clear()

        permissions.forEach {
            userPermissions.add(it)
        }

        logPermissions()
    }

    fun hasPermission(permission: String): Boolean {
        return userPermissions.contains(permission)
    }

    fun clearPermissions() {
        userPermissions.clear()
    }

    fun logPermissions() {

        if (userPermissions.isEmpty()) {
            Log.d("PermissionManager", "No permissions stored")
            return
        }

        Log.d("PermissionManager", "Stored Permissions:")

        userPermissions.forEach {
            Log.d("PermissionManager", it)
        }
    }
}