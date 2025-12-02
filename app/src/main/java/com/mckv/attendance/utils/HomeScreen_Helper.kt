package com.mckv.attendance.utils

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.*
//import java.time.OffsetDateTime
//import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun ExpiryCountdown(expiresAt: Long) {
    var remainingMillis by remember { mutableLongStateOf(expiresAt - System.currentTimeMillis()) }

    // Auto-update every second
    LaunchedEffect(expiresAt) {
        while (remainingMillis > 0) {
            kotlinx.coroutines.delay(1000)
            remainingMillis = expiresAt - System.currentTimeMillis()
        }
        remainingMillis = 0 // force non-negative
    }

    val totalSeconds = (remainingMillis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    Text(
        text = String.format("Time left %02d:%02d", minutes, seconds)
    )
}


fun parseExpiryDate(isoString: String?): Long {
    if (isoString.isNullOrBlank() || isoString == "0") {
        return 0L   // No active expiry → return 0 milliseconds
    }

    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        sdf.parse(isoString)?.time ?: 0L
    } catch (e: Exception) {
        0L
    }
}
