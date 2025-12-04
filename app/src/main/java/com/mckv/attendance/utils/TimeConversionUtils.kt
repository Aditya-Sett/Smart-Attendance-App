package com.mckv.attendance.utils

import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.Calendar
import android.util.Log
import java.util.Locale


// Time conversion utilities
fun convertUTCToISTMillis(utcString: String): Long {
    return try {
        if (utcString.isBlank()) return 0L

        // Parse UTC string
        val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        utcFormat.timeZone = TimeZone.getTimeZone("UTC")
        val utcDate = utcFormat.parse(utcString)

        if (utcDate == null) {
            // Try alternative format without milliseconds
            val altFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            altFormat.timeZone = TimeZone.getTimeZone("UTC")
            return altFormat.parse(utcString)?.time ?: 0L
        }

        // Convert to IST milliseconds
        val istCalendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        istCalendar.time = utcDate
        istCalendar.timeInMillis
    } catch (e: Exception) {
        Log.e("TimeConversion", "Error converting UTC to IST: ${e.message}")
        0L
    }
}

fun getCurrentISTMillis(): Long {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
    return calendar.timeInMillis
}

fun formatTimeRemaining(minutes: Int, seconds: Int): String {
    return String.format("%02d:%02d", minutes, seconds)
}