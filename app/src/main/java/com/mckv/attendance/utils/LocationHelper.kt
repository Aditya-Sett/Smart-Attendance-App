package com.mckv.attendance.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices

@SuppressLint("MissingPermission")
fun getCurrentLocation(context: Context, onLocationFetched: (Double, Double) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    try {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    onLocationFetched(location.latitude, location.longitude)
                } else {
                    // fallback: request a new location
                    val request = com.google.android.gms.location.LocationRequest.Builder(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 1000
                    ).setMaxUpdates(1).build()

                    fusedLocationClient.requestLocationUpdates(
                        request,
                        object : com.google.android.gms.location.LocationCallback() {
                            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                                fusedLocationClient.removeLocationUpdates(this)
                                val loc = result.lastLocation
                                if (loc != null) {
                                    onLocationFetched(loc.latitude, loc.longitude)
                                }
                            }
                        },
                        context.mainLooper
                    )
                }
            }
    } catch (e: SecurityException) {
        e.printStackTrace()
    }
}
