package com.mckv.attendance.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.*

@SuppressLint("MissingPermission")
fun getCurrentLocation(context: Context, onLocationFetched: (Double, Double) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    val request = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 2000L // every 2 sec if needed
    )
        .setWaitForAccurateLocation(true) // force best accuracy
        .setMaxUpdates(1) // only one fix
        .build()

    fusedLocationClient.requestLocationUpdates(
        request,
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)
                val loc = result.lastLocation
                if (loc != null) {
                    onLocationFetched(loc.latitude, loc.longitude)
                }
            }
        },
        Looper.getMainLooper()
    )
}
