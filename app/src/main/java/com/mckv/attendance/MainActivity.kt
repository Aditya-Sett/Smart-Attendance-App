package com.mckv.attendance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mckv.attendance.ui.theme.SmartAttendanceAppTheme
import android.app.PendingIntent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import android.content.Intent
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.mckv.attendance.data.remote.RetrofitClient
import androidx.navigation.compose.*
import com.mckv.attendance.data.local.SessionManager
import com.mckv.attendance.receiver.GeofenceBroadcastReceiver
import com.mckv.attendance.ui.screens.AddScheduleScreen
import com.mckv.attendance.ui.screens.ApproveAbsenceScreen
import com.mckv.attendance.ui.screens.LoginScreen
import com.mckv.attendance.ui.screens.AttendanceSummaryScreen
import com.mckv.attendance.ui.screens.ConsiderAbsenceScreen
import com.mckv.attendance.ui.screens.HomeScreen
import com.mckv.attendance.ui.screens.MainHomeScreen
import com.mckv.attendance.ui.screens.ScheduleScreen
import com.mckv.attendance.ui.screens.StudentsAttendanceSummaryScreen
import com.mckv.attendance.ui.screens.TakeAttendanceScreen
import com.mckv.attendance.ui.screens.ExportAttendanceScreen
import com.mckv.attendance.ui.screens.TeacherScreen
import com.mckv.attendance.ui.screens.AdminDashboard
import com.mckv.attendance.ui.screens.ClassroomListScreen
import com.mckv.attendance.ui.screens.AddClassroomScreen
import com.mckv.attendance.ui.screens.AttendanceRecordsScreen
import com.mckv.attendance.ui.screens.SplashScreen

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

        //SESSION MANAGER IS APP'S MEMORY FOR LOGIN/SESSION DATA
        SessionManager.init(applicationContext) // ✅ Initialize session storage

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

        lifecycleScope.launch {
            try {
                val schedule = RetrofitClient.instance.getScheduleByDepartment("CSE")
                Log.d("ScheduleData", schedule.toString())
            } catch (e: Exception) {
                Log.e("API_ERROR", "Failed to fetch schedule: ${e.message}")
            }
        }



        setContent {
            SmartAttendanceAppTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "splash_screen") {

                    composable("splash_screen") {
                        SplashScreen(navController)
                    }

                    composable("main_home") {
                        MainHomeScreen(navController)
                    }
                    composable("login_screen/{role}") { backStackEntry ->
                        val role = backStackEntry.arguments?.getString("role") ?: "ROLE_STUDENT"
                        LoginScreen(navController, roleFromNav = role)
                    }
                    composable("home") {
                        HomeScreen(navController)
                    }
                    composable("schedule") {
                        //ScheduleScreen(department = "CSE")
                        val department = SessionManager.department
                        if (department.isNullOrEmpty()) {
                            Text("Department info not available. Please re-login.")
                        } else {
                            ScheduleScreen(department = department)
                        }
                    }
                    composable("teacher") {
                        TeacherScreen(navController)
                    }
                    composable("admin_dashboard") {
                        AdminDashboard(navController)
                    }
                    composable("classroomList") {
                        ClassroomListScreen(navController)
                    }
                    composable("addClassroom") {
                        AddClassroomScreen(navController)
                    }
                    composable("add_schedule") {
                        AddScheduleScreen(navController)
                    }
                    composable("take_attendance") {
                        TakeAttendanceScreen(navController)
                    }
                    composable("attendance_records") {
                        AttendanceRecordsScreen(navController)
                    }
                    composable("export_attendance") {
                        ExportAttendanceScreen(navController)
                    }
                    composable("consider_absence") {
                        ConsiderAbsenceScreen(navController)
                    }
                    composable("approve_absence") { ApproveAbsenceScreen(navController) }
                    composable("attendance_summary") {
                        //val context = LocalContext.current
                        //val sessionManager = SessionManager(context.applicationContext)
                        val studentId = SessionManager.studentId ?: ""
                        val department = SessionManager.department ?: ""
                        AttendanceSummaryScreen(
                            studentId = studentId,
                            department = department,
                            apiService = RetrofitClient.instance
                        )
                    }
                    composable("students_attendance_summary") {
                        StudentsAttendanceSummaryScreen(
                            apiService = RetrofitClient.instance,
                            navController = navController
                        )
                    }

                }
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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SmartAttendanceAppTheme {
        Greeting("Android")
    }
}