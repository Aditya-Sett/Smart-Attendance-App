package com.mckv.attendance.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mckv.attendance.data.local.SessionManager
import com.mckv.attendance.data.remote.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// --- Helper: Build fingerprint from scan results ---
private fun buildWifiFingerprint(context: Context, topN: Int = 8): JSONArray {
    val jsonArr = JSONArray()
    try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        // Trigger a scan - may be throttled on newer Android versions.
        try {
            wifiManager.startScan()
        } catch (e: SecurityException) {
            // startScan may throw if permission missing
            Log.e("WiFiScan", "startScan failed: ${e.message}")
        }

        // Get last known scan results (may be empty if there was no recent scan)
        val scans = try {
            wifiManager.scanResults ?: emptyList()
        } catch (se: SecurityException) {
            emptyList()
        }

        // Sort by RSSI descending and take top N unique BSSIDs
        val top = scans
            .sortedByDescending { it.level }
            .distinctBy { it.BSSID?.lowercase() ?: "" }
            .take(topN)

        for (r in top) {
            val obj = JSONObject()
            obj.put("SSID", r.SSID ?: "")
            obj.put("BSSID", r.BSSID ?: "")
            obj.put("level", r.level) // negative dBm values
            jsonArr.put(obj)
        }
    } catch (e: Exception) {
        Log.e("WiFiFingerprint", "error building fingerprint: ${e.message}")
    }
    return jsonArr
}

// --- Permission utility (request multiple) ---
@Composable
private fun rememberWifiScanPermissions(): Pair<Boolean, () -> Unit> {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(false)
    }

    val permissions = mutableListOf<String>().apply {
        // Location permission is required for Wi-Fi scans on most Android versions
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        // Starting Android 12 / API 31, NEARBY_WIFI_DEVICES is recommended for direct wifi access
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // results is a Map<String, Boolean>
        granted = results.values.all { it }
        if (!granted) {
            Toast.makeText(context, "Location/Wi-Fi permission required for fingerprinting", Toast.LENGTH_SHORT).show()
        }
    }

    // initial permission check
    LaunchedEffect(Unit) {
        val ok = permissions.all { p ->
            ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
        }
        granted = ok
    }

    // function to launch permission request
    val request: () -> Unit = {
        launcher.launch(permissions.toTypedArray())
    }

    return Pair(granted, request)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeAttendanceScreen(navController: androidx.navigation.NavHostController) {
    val context = LocalContext.current
    val teacherId = SessionManager.teacherId ?: "Unknown"

    var department by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var responseMessage by remember { mutableStateOf<String?>(null) }

    // permission helper: returns (granted, requestFunction)
    val (hasPermissions, requestPermissions) = rememberWifiScanPermissions()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teacher Panel") },
                navigationIcon = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = teacherId,
                        onValueChange = {},
                        label = { Text("Teacher ID") },
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = department,
                        onValueChange = { department = it },
                        label = { Text("Department") },
                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        leadingIcon = { Icon(Icons.Default.Book, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (teacherId == "Unknown" || department.isBlank() || subject.isBlank()) {
                                Toast.makeText(context, "⚠ Fill all fields", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // Ensure permissions
                            if (!hasPermissions) {
                                requestPermissions()
                                return@Button
                            }

                            // Build Wi-Fi fingerprint (top 8 APs)
                            val wifiFingerprint = buildWifiFingerprint(context, topN = 8)

                            if (wifiFingerprint.length() == 0) {
                                Toast.makeText(context, "No Wi-Fi networks found. Try again or enable Wi-Fi scanning.", Toast.LENGTH_LONG).show()
                                return@Button
                            }

                            // Build payload
                            val json = JSONObject().apply {
                                put("teacherId", teacherId)
                                put("department", department)
                                put("subject", subject)
                                put("wifiFingerprint", wifiFingerprint) // JSONArray
                            }

                            val requestBody = json.toString()
                                .toRequestBody("application/json".toMediaTypeOrNull())

                            val call = RetrofitClient.instance.generateCode(requestBody)
                            call.enqueue(object : Callback<ResponseBody> {
                                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                    if (response.isSuccessful) {
                                        val result = response.body()?.string()
                                        responseMessage = result
                                    } else {
                                        responseMessage = "⚠️ Server Error: ${response.errorBody()?.string()}"
                                    }
                                }

                                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                    responseMessage = "🚫 Network error: ${t.message}"
                                }
                            })
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Attendance Code")
                    }
                }
            }

            // --- Show generated code ---
            responseMessage?.let { rawResponse ->
                val formatted = rawResponse
                    .replace("{", "")
                    .replace("}", "")
                    .replace("\"", "")
                    .replace(",", "\n")
                    .replace(":", " : ")
                    .trim()

                AlertDialog(
                    onDismissRequest = { responseMessage = null },
                    confirmButton = {
                        Button(onClick = { responseMessage = null }) {
                            Text("OK")
                        }
                    },
                    title = { Text("Attendance Code Generated") },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Share this code with your students:")
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = formatted,
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1976D2)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Valid for 2–3 minutes", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    },
                    shape = MaterialTheme.shapes.large
                )
            }
        }
    }
}
