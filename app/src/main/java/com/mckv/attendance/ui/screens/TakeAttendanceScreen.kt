package com.mckv.attendance.ui.screens
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.app.Activity
//import android.bluetooth.BluetoothAdapter
//import android.bluetooth.BluetoothManager
//import android.content.Context
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.net.wifi.WifiManager
//import android.os.Build
//import android.util.Log
//import android.widget.Toast
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Menu
//import androidx.compose.material.icons.filled.Person
//import androidx.compose.material.icons.filled.QrCode
//import androidx.compose.material.icons.filled.School
//import androidx.compose.material3.Button
//import androidx.compose.material3.Card
//import androidx.compose.material3.CardDefaults
//import androidx.compose.material3.DropdownMenuItem
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.ExposedDropdownMenuBox
//import androidx.compose.material3.ExposedDropdownMenuDefaults
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.OutlinedTextField
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.material3.TopAppBar
//import androidx.compose.material3.TopAppBarDefaults
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.core.content.ContextCompat
//import com.mckv.attendance.R
//import com.mckv.attendance.data.local.SessionManager
//import com.mckv.attendance.data.remote.RetrofitClient
//import com.mckv.attendance.utils.DepartmentAutoComplete
//import com.mckv.attendance.utils.SubjectAutoComplete
//import com.mckv.attendance.utils.ensureBluetoothPermissions
//import com.mckv.attendance.utils.getCurrentISTMillis
//import com.mckv.attendance.utils.startBleAdvertising
//import kotlinx.coroutines.delay
//import okhttp3.MediaType.Companion.toMediaTypeOrNull
//import okhttp3.RequestBody.Companion.toRequestBody
//import okhttp3.ResponseBody
//import org.json.JSONArray
//import org.json.JSONObject
//import retrofit2.Call
//import retrofit2.Callback
//import retrofit2.Response
//import java.text.SimpleDateFormat
//import java.util.Locale
//
//@SuppressLint("MissingPermission")
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun TakeAttendanceScreen(navController: androidx.navigation.NavHostController) {
//    val context = LocalContext.current
//    val activity = context as Activity
//
//    //FETCHING THE TEACHER ID THAT STORED AFTER LOGIN IN SESSION MANAGER
//    val teacherId = SessionManager.userDetails?.userId ?: "Unknown"
//
//    var department by remember { mutableStateOf("") }
//    var subject by remember { mutableStateOf("") }
//    var expanded by remember { mutableStateOf(false) }
//    val semOptions = listOf("1st", "2nd", "3rd", "4th","5th", "6th", "7th", "8th")
//    var semester by remember { mutableStateOf("") }
//    var responseMessage by remember { mutableStateOf<String?>(null) }
//    var responseList by remember { mutableStateOf(mutableListOf<String>()) }
//    var bluetoothUuid by remember { mutableStateOf<String?>(null) }
//
//    // permission helper: returns (granted, requestFunction)
//    val (hasPermissions, requestPermissions) = rememberWifiScanPermissions()
//
//    //IT RUN WHEN SCREEN FIRST OPEN
//    LaunchedEffect(Unit) {
//
//        val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(java.util.Date())
//        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date())
//        println("currentDay: ${currentDay}")
//        println("currentTime: ${currentTime}")
//
//        val json = JSONObject().apply {
//            put("teacher_id", teacherId)
//            put("day", currentDay)
//            put("time", currentTime)
//        }
//
//        val requestBody = json.toString()
//            .toRequestBody("application/json".toMediaTypeOrNull())
//
//        RetrofitClient.analysisInstance.getCurrentClass(requestBody)
//            .enqueue(object : Callback<ResponseBody> {
//
//                override fun onResponse(
//                    call: Call<ResponseBody>,
//                    response: Response<ResponseBody>
//                ) {
//                    if (response.isSuccessful) {
//
//                        val result = response.body()?.string()
//                        if (result != null) {
//
//                            val obj = JSONObject(result)
//                            val success = obj.optBoolean("success", false)
//
//                            if (success) {
//                                // ✅ AUTO FILL
//                                department = obj.optString("department", "")
//                                subject = obj.optString("subject", "")
//                                semester = when (obj.optInt("semester", 0)) {
//                                    1 -> "1st"
//                                    2 -> "2nd"
//                                    3 -> "3rd"
//                                    4 -> "4th"
//                                    5 -> "5th"
//                                    6 -> "6th"
//                                    7 -> "7th"
//                                    8 -> "8th"
//                                    else -> ""
//                                }
//                            } else {
//                                // ❌ No class found → do nothing (manual input stays)
//                                Log.d("AUTO_FILL", "No class found")
//                            }
//                        }
//
//                    } else {
//                        Log.e("AUTO_FILL", "Server error")
//                    }
//                }
//
//                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                    Log.e("AUTO_FILL", "API failed: ${t.message}")
//                }
//            })
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Teacher Panel") },
//                navigationIcon = {
//                    IconButton(onClick = { /* TODO */ }) {
//                        Icon(Icons.Default.Menu, contentDescription = "Menu")
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = Color(0xFF1976D2),
//                    titleContentColor = Color.White,
//                    navigationIconContentColor = Color.White
//                )
//            )
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//                .padding(16.dp),
//            verticalArrangement = Arrangement.Top
//        ) {
//            Card(
//                modifier = Modifier.fillMaxWidth(),
//                shape = RoundedCornerShape(16.dp),
//                elevation = CardDefaults.cardElevation(8.dp)
//            ) {
//                Column(
//                    modifier = Modifier.padding(16.dp),
//                    verticalArrangement = Arrangement.spacedBy(12.dp)
//                ) {
//                    OutlinedTextField(
//                        value = teacherId,
//                        onValueChange = {},
//                        label = { Text("Teacher ID") },
//                        readOnly = true,
//                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
//                        modifier = Modifier.fillMaxWidth()
//                    )
//
//                    /*OutlinedTextField(
//                        value = department,
//                        onValueChange = { department = it },
//                        label = { Text("Department") },
//                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
//                        modifier = Modifier.fillMaxWidth()
//                    )*/
//                    DepartmentAutoComplete(
//                        department = department,
//                        onDepartmentChange = { department = it }
//                    )
//
//                    /*OutlinedTextField(
//                        value = subject,
//                        onValueChange = { subject = it },
//                        label = { Text("Subject") },
//                        leadingIcon = { Icon(Icons.Default.Book, contentDescription = null) },
//                        modifier = Modifier.fillMaxWidth()
//                    )*/
//                    SubjectAutoComplete(
//                        subject = subject,
//                        onSubjectChange = { subject = it }
//                    )
//
//                    // --- Dropdown for Class Name ---
////                    var expanded by remember { mutableStateOf(false) }
////                    val semOptions = listOf("1st", "2nd", "3rd", "4th","5th", "6th", "7th", "8th")
////                    var semester by remember { mutableStateOf("") }
//
//                    ExposedDropdownMenuBox(
//                        expanded = expanded,
//                        onExpandedChange = { expanded = !expanded }
//                    ) {
//                        OutlinedTextField(
//                            value = semester,
//                            onValueChange = {},
//                            readOnly = true,
//                            label = { Text("Semester") },
//                            leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
//                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
//                            modifier = Modifier
//                                .menuAnchor()
//                                .fillMaxWidth()
//                        )
//
//                        ExposedDropdownMenu(
//                            expanded = expanded,
//                            onDismissRequest = { expanded = false }
//                        ) {
//                            semOptions.forEach { option ->
//                                DropdownMenuItem(
//                                    text = { Text(option) },
//                                    onClick = {
//                                        semester = option
//                                        expanded = false
//                                    }
//                                )
//                            }
//                        }
//                    }
//
//                    Button(
//                        onClick = {
//                            if (teacherId == "Unknown" || department.isBlank() || subject.isBlank() || semester.isBlank()) {
//                                Toast.makeText(context, "⚠ Fill all fields", Toast.LENGTH_SHORT).show()
//                                return@Button
//                            }
//
//                            // Ensure permissions
//                            if (!hasPermissions) {
//                                requestPermissions()
//                                return@Button
//                            }
//
//                            // Build Wi-Fi fingerprint (top 8 APs)
//                            val wifiFingerprint = buildWifiFingerprint(context, topN = 8)
//
//                            /*if (wifiFingerprint.length() == 0) {
//                                Toast.makeText(context, "No Wi-Fi networks found. Try again or enable Wi-Fi scanning.", Toast.LENGTH_LONG).show()
//                                return@Button
//                            }*/
//
//                            // Checking whether bluetooth on or off ,, if off then request user to turn on
//                            if (!ensureBluetoothPermissions(activity)) {
//                                return@Button     // wait for user to grant permission
//                            }
//                            val bluetoothAdapter: BluetoothAdapter? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                                // Android 12+
//                                val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
//                                bluetoothManager.adapter
//                            } else {
//                                // Android < 12
//                                BluetoothAdapter.getDefaultAdapter()
//                            }
//
//                            if (bluetoothAdapter == null) {
//                                println ("Device does NOT support Bluetooth")
//                                return@Button
//                            } else {
//                                if (bluetoothAdapter.isEnabled) {
//                                    // Bluetooth is ON
//                                    println("Bluetooth is on")
//                                } else {
//                                    // Bluetooth is OFF
//                                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//                                    activity.startActivityForResult(intent, 1001)
//                                    return@Button
//                                }
//                            }
//
//                            startBleAdvertising(
//                                context = context,
//                                activity = activity
//                            ){ uuid ->
//                                bluetoothUuid = uuid
//                                println("Send UUID to backend → $uuid")
//                                Toast.makeText(context,"Send UUID to backend → $uuid", Toast.LENGTH_LONG).show()
//                                val json = JSONObject().apply {
//                                    put("teacherId", teacherId)
//                                    put("department", department)
//                                    put("subject", subject)
//                                    put("wifiFingerprint", wifiFingerprint) // JSONArray
//                                    put("sem", semester)
//                                    /*bluetoothUuid?.let {
//                                        put("bluetoothUuid", it)
//                                    }*/
//                                    put("bluetoothUuid", bluetoothUuid)
//                                }
//                                println("json go to backend --->  $json")
//
//                                val requestBody = json.toString()
//                                    .toRequestBody("application/json".toMediaTypeOrNull())
//
//                                println("generated code requestBody --->  $requestBody")
//                                Log.d("JSON", requestBody.toString())
//                                Toast.makeText(context,"$requestBody", Toast.LENGTH_LONG).show()
//
//                                val call = RetrofitClient.instance.generateCode(requestBody)
//                                call.enqueue(object : Callback<ResponseBody> {
//                                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
//                                        if (response.isSuccessful) {
//                                            val result = response.body()?.string()
//                                            //responseMessage = result
//                                            if (result != null) {
//                                                responseList = (responseList + result).toMutableList()
//                                            }
//                                        } else {
//                                            responseMessage = "⚠️ Server Error: ${response.errorBody()?.string()}"
//                                        }
//                                    }
//
//                                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                                        responseMessage = "🚫 Network error: ${t.message}"
//                                    }
//                                })
//                            }
//                        },
//                        modifier = Modifier.fillMaxWidth(),
//                        shape = RoundedCornerShape(12.dp)
//                    ) {
//                        Icon(Icons.Default.QrCode, contentDescription = null)
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text("Generate Attendance Code")
//                    }
//                }
//            }
//
//            if (responseList.isNotEmpty())
//            {
//
//                LazyColumn {
//                    items (responseList) { res -> // error occur
//
//                        CompactCodeCard(
//                            //response = responseMessage!!,
//                            response = res, // error occur
//                            onClose = {
//
//                                // CALL Close API
//                                val jsonReq = JSONObject().apply {
//                                    val obj = JSONObject(res)
//                                    put("teacherId", SessionManager.userDetails?.userId)
//                                    //put("department", JSONObject(responseMessage!!).getString("department"))
//                                    //put("subject", JSONObject(responseMessage!!).getString("subject"))
//                                    //put("className", JSONObject(responseMessage!!).getString("className"))
//                                    put("department", obj.getString("department"))
//                                    put("subject", obj.getString("subject"))
//                                    put("className", obj.getString("className"))
//                                }
//
//                                val body = jsonReq.toString()
//                                    .toRequestBody("application/json".toMediaTypeOrNull())
//
//                                RetrofitClient.instance.closeCode(body)
//                                    .enqueue(object : Callback<ResponseBody> {
//                                        override fun onResponse(
//                                            call: Call<ResponseBody>,
//                                            response: Response<ResponseBody>
//                                        ) {
//                                            val r = response.body()?.string()
//                                            if (r != null && JSONObject(r).getBoolean("success")) {
//                                                //responseMessage = null
//                                                responseList = responseList.toMutableList().also { it.remove(res) }
//                                            }
//                                        }
//                                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {}
//                                    })
//                            }
//                        )
//                    }
//                }
//            }
//
//        }
//    }
//}
//
//@Composable
//fun CompactCodeCard(response: String, onClose: () -> Unit) {
//    val json = JSONObject(response)
//    val code = json.getString("code")
//    val department = json.getString("department")
//    val subject = json.getString("subject")
//    val className = json.getString("className")
//    val generatedAt = json.getString("generatedAt")
//    val expiresAt = json.getString("expiresAt")
//
//    var remainingTime by remember { mutableStateOf(0L) }
//
//    // TIMER CALCULATION
//    LaunchedEffect(Unit) {
//        val format = SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.getDefault())
//        val genTime = format.parse(generatedAt)?.time ?: 0L
//        val expTime = format.parse(expiresAt)?.time ?: 0L
//        val currentIST = getCurrentISTMillis()
//        //remainingTime = maxOf((expTime - genTime) / 1000, 0)
//        remainingTime = maxOf((expTime - currentIST) / 1000, 0)
//        //Log.d("EXP","$expTime")
//        //Log.d("GEN","$genTime")
//        println("@@@  EXP $expTime")
//        println("@@@ GEN $genTime")
//        println("@@@ currentIST $currentIST")
//    }
//
//    LaunchedEffect(remainingTime) {
//        while (remainingTime > 0) {
//            delay(1000)
//            remainingTime--
//            Log.d("Remaining time","$remainingTime")
//        }
//    }
//
//    val timerText = String.format("%02d:%02d", remainingTime / 60, remainingTime % 60)
//
//    // ---- COMPACT CARD UI ----
//    Card(
//        modifier = Modifier
//            .padding(12.dp)
//            .fillMaxWidth(),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(containerColor = Color.White),
//        elevation = CardDefaults.cardElevation(6.dp)
//    ) {
//
//        Column(Modifier.padding(16.dp)) {
//
//            // TOP ROW
//            Row(
//                Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//
//                Text(
//                    text = "$code",
//                    style = MaterialTheme.typography.displaySmall.copy(
//                        fontWeight = FontWeight.Bold,
//                        color = Color(0xFF1976D2)
//                    )
//
//                )
//
//                /*IconButton(onClick = onClose) {
//                    Icon(Icons.Default.Close, contentDescription = "Close")
//                }*/
//                if (remainingTime.toInt() != 0) {
//                    IconButton(
//                        onClick = onClose,
//                        modifier = Modifier.padding(end = 6.dp).size(30.dp) // Button Size
//                    ) {
//                        Icon(
//                            painter = painterResource(id = R.drawable.close_circle),
//                            contentDescription = "Close",
//                            tint = Color.Unspecified,   // IMPORTANT: do not recolor XML
//                            modifier = Modifier.size(50.dp) // Icon Size
//                        )
//                    }
//                }
//                else {
//                    Text(
//                        text = "Expired",
//                        style = MaterialTheme.typography.displaySmall.copy(
//                            fontWeight = FontWeight.Bold,
//                            fontSize = 20.sp,
//                            color = Color(0xFFFF0000)
//                        )
//
//                    )
//                }
//            }
//
//            Spacer(Modifier.height(2.dp))
//
//            // BOTTOM ROW
//            Row(
//                Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//
//                Column {
//                    Text("$department | $subject", fontWeight = FontWeight.Medium)
//                    //Text("$subject", fontWeight = FontWeight.Medium)
//                }
//
//                Text(
//                    text = timerText,
//                    color = if (remainingTime < 20) Color.Red else Color(0xFF388E3C),
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Bold
//                )
//            }
//        }
//    }
//}
//
//
//
//// --- Helper: Build fingerprint from scan results ---
//private fun buildWifiFingerprint(context: Context, topN: Int = 8): JSONArray {
//    val jsonArr = JSONArray()
//    try {
//        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//        // Trigger a scan - may be throttled on newer Android versions.
//        try {
//            wifiManager.startScan()
//        } catch (e: SecurityException) {
//            // startScan may throw if permission missing
//            Log.e("WiFiScan", "startScan failed: ${e.message}")
//        }
//
//        // Get last known scan results (may be empty if there was no recent scan)
//        val scans = try {
//            wifiManager.scanResults ?: emptyList()
//        } catch (se: SecurityException) {
//            emptyList()
//        }
//
//        // Sort by RSSI descending and take top N unique BSSIDs
//        val top = scans
//            .sortedByDescending { it.level }
//            .distinctBy { it.BSSID?.lowercase() ?: "" }
//            .take(topN)
//
//        for (r in top) {
//            val obj = JSONObject()
//            obj.put("SSID", r.SSID ?: "")
//            obj.put("BSSID", r.BSSID ?: "")
//            obj.put("level", r.level) // negative dBm values
//            jsonArr.put(obj)
//        }
//    } catch (e: Exception) {
//        Log.e("WiFiFingerprint", "error building fingerprint: ${e.message}")
//    }
//    return jsonArr
//}
//@Composable
//private fun rememberWifiScanPermissions(): Pair<Boolean, () -> Unit> {
//    val context = LocalContext.current
//    var granted by remember { mutableStateOf(false) }
//
//    val permissions = mutableListOf(
//        Manifest.permission.ACCESS_FINE_LOCATION
//    )
//
//    /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)*/
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
//    {
//        permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
//    }
//
//    val launcher = rememberLauncherForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { results ->
//
//        val fineLocation = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
//        val wifiNearby =
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
//                results[Manifest.permission.NEARBY_WIFI_DEVICES] == true
//            else true
//
//        granted = fineLocation && wifiNearby
//
//        if (!granted) {
//            Toast.makeText(
//                context,
//                "Location & Wi-Fi permissions required for fingerprinting",
//                Toast.LENGTH_SHORT
//            ).show()
//        }
//    }
//
//    // Initial check
//    LaunchedEffect(Unit) {
//        val fineLocation =
//            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
//                    PackageManager.PERMISSION_GRANTED
//
//        val wifiNearby =
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
//                ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) ==
//                        PackageManager.PERMISSION_GRANTED
//            else true
//
//        granted = fineLocation && wifiNearby
//    }
//
//    return granted to { launcher.launch(permissions.toTypedArray()) }
//}