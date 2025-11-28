package com.mckv.attendance.data.local

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.mckv.attendance.data.local.SessionManager
import com.mckv.attendance.utils.JwtUtils
import kotlinx.coroutines.delay

object TokenExpiryManager {
    private var isDialogShowing = false

    fun shouldShowExpiryDialog(): Boolean {
        val token = SessionManager.authToken
        return !isDialogShowing &&
                !token.isNullOrEmpty() &&
                JwtUtils.isTokenExpired(token)
    }

    fun setDialogShowing(showing: Boolean) {
        isDialogShowing = showing
    }

    fun isTokenExpired(): Boolean {
        val token = SessionManager.authToken
        return !token.isNullOrEmpty() && JwtUtils.isTokenExpired(token)
    }
}