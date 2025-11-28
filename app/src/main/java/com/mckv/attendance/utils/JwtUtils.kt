package com.mckv.attendance.utils

import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

object JwtUtils {

    fun decodeJwt(token: String): JwtData? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) {
                Log.e("JWT", "Invalid JWT token format")
                return null
            }

            val payload = parts[1]
            val json = String(Base64.decode(payload, Base64.URL_SAFE), Charsets.UTF_8)
            val jsonObject = JSONObject(json)

            JwtData(
                subject = jsonObject.optString("sub", ""),
                expiration = jsonObject.optLong("exp", 0L),
                issuedAt = jsonObject.optLong("iat", 0L),
                userId = jsonObject.optString("userId", ""),
                role = jsonObject.optString("role", "")
            )
        } catch (e: Exception) {
            Log.e("JWT", "Error decoding JWT: ${e.message}")
            null
        }
    }

    fun getTokenExpiryTime(token: String): Long {
        return decodeJwt(token)?.expiration ?: 0L
    }

    fun isTokenExpired(token: String): Boolean {
        val expiryTime = getTokenExpiryTime(token)
        if (expiryTime == 0L) return true

        val currentTime = System.currentTimeMillis() / 1000 // JWT uses seconds
        return currentTime >= expiryTime
    }

    fun getTokenExpiryDate(token: String): String {
        val expiryTime = getTokenExpiryTime(token)
        if (expiryTime == 0L) return "Unknown"

        val date = Date(expiryTime * 1000) // Convert seconds to milliseconds
        val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
        return formatter.format(date)
    }

    fun getTimeUntilExpiry(token: String): String {
        val expiryTime = getTokenExpiryTime(token)
        if (expiryTime == 0L) return "Unknown"

        val currentTime = System.currentTimeMillis() / 1000
        val secondsLeft = expiryTime - currentTime

        return when {
            secondsLeft <= 0 -> "Expired"
            secondsLeft < 60 -> "${secondsLeft.toInt()} seconds"
            secondsLeft < 3600 -> "${(secondsLeft / 60).toInt()} minutes"
            secondsLeft < 86400 -> "${(secondsLeft / 3600).toInt()} hours"
            else -> "${(secondsLeft / 86400).toInt()} days"
        }
    }

    fun printTokenInfo(token: String) {
        val jwtData = decodeJwt(token)
        if (jwtData != null) {
            Log.d("JWT", "🔐 Token Information:")
            Log.d("JWT", "   - Subject: ${jwtData.subject}")
            Log.d("JWT", "   - User ID: ${jwtData.userId}")
            Log.d("JWT", "   - Role: ${jwtData.role}")
            Log.d("JWT", "   - Issued At: ${Date(jwtData.issuedAt * 1000)}")
            Log.d("JWT", "   - Expires At: ${Date(jwtData.expiration * 1000)}")
            Log.d("JWT", "   - Time Until Expiry: ${getTimeUntilExpiry(token)}")
            Log.d("JWT", "   - Is Expired: ${isTokenExpired(token)}")
        } else {
            Log.e("JWT", "❌ Failed to decode token")
        }
    }
}

data class JwtData(
    val subject: String,
    val expiration: Long, // in seconds
    val issuedAt: Long,   // in seconds
    val userId: String,
    val role: String
)