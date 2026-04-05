package com.mckv.attendance.data.local

import com.mckv.attendance.utils.JwtUtils

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