package com.mckv.attendance

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.mckv.attendance.navigation.AppNavigation
import com.mckv.attendance.receiver.GeofenceBroadcastReceiver
import com.mckv.attendance.ui.theme.SmartAttendanceAppTheme


class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofencePendingIntent: PendingIntent



    // Declare launcher at class level
    private val     requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            getCurrentLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }


    //ENTRY POINT OF MAIN ACTIVITY
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //enableEdgeToEdge() IS A ANDROID API CALL THAT MAKES APP AREA EXTENDS SYSTEM BAR'S AREA (STATUS BAR AT TOP, NAVIGATION BAR AT BOTTOM)
        enableEdgeToEdge()

        // 1) FUSED LOCATION PROVIDER IS  A PART OF GOOGLE PLAY SERVICE (com.google.android.gms.location)
        // 2) COMBINE MULTIPLE LOCATION SOURCE:  GPS (high accuracy), WIFI, CELL TOWER, BLUETOOTH BEACON
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        geofencingClient = LocationServices.getGeofencingClient(this)
        geofencePendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(this, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )


        requestLocationPermission()

        setContent {
            SmartAttendanceAppTheme {
                val navController = rememberNavController()

                //CALL THE CENTRAL NAVIGATION
                AppNavigation(navController = navController)
            }
        }
    }

    private fun requestLocationPermission() {
//        Toast.makeText(this,"RequestLocationPermission Hit",Toast.LENGTH_LONG).show()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            )
        } else {
            getCurrentLocation()
        }
    }

    //GET CURRENT LOCATION
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission was removed or denied — don’t proceed
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                Toast.makeText(this, "Lat: ${it.latitude}\n Lon: ${it.longitude}", Toast.LENGTH_LONG).show()
            }
            addGeofence()
        }
    }


    private fun addGeofence() {
        val geofence = Geofence.Builder()
            .setRequestId("sample_geofence")
            .setCircularRegion(
                22.842058, 88.359271, 100f
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
            .addOnSuccessListener {
                Log.d("Geofence", "Geofence added")
                Toast.makeText(this, " Geofence Added\nLat: 22.842058\n Lon: 88.359271", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                Log.e("Geofence", "Error: ${it.message}")
            }
    }
}
