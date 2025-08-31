package com.mckv.attendance.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import android.os.Build


class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent)
        if (event == null || event.hasError()) {
            Log.e("GeofenceReceiver", "Error: ${event?.errorCode}")
            return
        }

        val geofenceList = event.triggeringGeofences
        if (geofenceList != null) {
            for (geofence in geofenceList) {
                Log.d("GeofenceReceiver", "Geofence triggered: ${geofence.requestId}")
            }
        }

        when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Log.d("GeofenceReceiver", "Entered geofence")
                showNotification(context, "Class Alert", "Your Physics class starts at 9:30 AM in A113.")
            }
            Geofence.GEOFENCE_TRANSITION_EXIT ->
                Log.d("GeofenceReceiver", "Exited geofence")
            else ->
                Log.d("GeofenceReceiver", "Other transition")
        }
    }
    private fun showNotification(context: Context, title: String, message: String) {
        val channelId = "geo_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Geofence Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        notificationManager.notify(1001, builder.build())
    }
}
