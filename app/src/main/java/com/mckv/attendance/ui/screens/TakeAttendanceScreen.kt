package com.mckv.attendance.ui.screens

import com.mckv.attendance.utils.startBleAdvertising
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
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
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.mckv.attendance.R
import java.text.SimpleDateFormat
import java.util.Locale

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
/*@Composable
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
}*/
@Composable
private fun rememberWifiScanPermissions(): Pair<Boolean, () -> Unit> {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(false) }

    val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)*/
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
    {
        permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->

        val fineLocation = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val wifiNearby =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                results[Manifest.permission.NEARBY_WIFI_DEVICES] == true
            else true

        granted = fineLocation && wifiNearby

        if (!granted) {
            Toast.makeText(
                context,
                "Location & Wi-Fi permissions required for fingerprinting",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Initial check
    LaunchedEffect(Unit) {
        val fineLocation =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED

        val wifiNearby =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) ==
                        PackageManager.PERMISSION_GRANTED
            else true

        granted = fineLocation && wifiNearby
    }

    return granted to { launcher.launch(permissions.toTypedArray()) }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeAttendanceScreen(navController: androidx.navigation.NavHostController) {
    val context = LocalContext.current
    val activity = context as Activity
    val teacherId = SessionManager.teacherId ?: "Unknown"

    var department by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var responseMessage by remember { mutableStateOf<String?>(null) }
    var responseList by remember { mutableStateOf(mutableListOf<String>()) }
    var bluetoothUuid by remember { mutableStateOf<String?>(null) }

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

                    // --- Dropdown for Class Name ---
                    var expanded by remember { mutableStateOf(false) }
                    val classOptions = listOf("1st Year", "2nd Year", "3rd Year", "4th Year")
                    var className by remember { mutableStateOf("") }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = className,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Class") },
                            leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            classOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        className = option
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (teacherId == "Unknown" || department.isBlank() || subject.isBlank() || className.isBlank()) {
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

                            startBleAdvertising(
                                context = context,
                                activity = activity
                            ){ uuid ->
                                bluetoothUuid = uuid
                                println("Send UUID to backend → $uuid")
                                Toast.makeText(context,"Send UUID to backend → $uuid", Toast.LENGTH_LONG).show()
                                val json = JSONObject().apply {
                                    put("teacherId", teacherId)
                                    put("department", department)
                                    put("subject", subject)
                                    put("wifiFingerprint", wifiFingerprint) // JSONArray
                                    put("className", className)
                                    /*bluetoothUuid?.let {
                                        put("bluetoothUuid", it)
                                    }*/
                                    put("bluetoothUuid", bluetoothUuid)
                                }
                                println("json go to backend --->  $json")

                                val requestBody = json.toString()
                                    .toRequestBody("application/json".toMediaTypeOrNull())

                                println("generated code requestBody --->  $requestBody")
                                Log.d("JSON", requestBody.toString())
                                Toast.makeText(context,"$requestBody", Toast.LENGTH_LONG).show()

                                val call = RetrofitClient.instance.generateCode(requestBody)
                                call.enqueue(object : Callback<ResponseBody> {
                                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                        if (response.isSuccessful) {
                                            val result = response.body()?.string()
                                            //responseMessage = result
                                            if (result != null) {
                                                responseList = (responseList + result).toMutableList()
                                            }
                                        } else {
                                            responseMessage = "⚠️ Server Error: ${response.errorBody()?.string()}"
                                        }
                                    }

                                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                        responseMessage = "🚫 Network error: ${t.message}"
                                    }
                                })
                            }


                            // Build payload
                            /*val json = JSONObject().apply {
                                put("teacherId", teacherId)
                                put("department", department)
                                put("subject", subject)
                                put("wifiFingerprint", wifiFingerprint) // JSONArray
                                put("className", className)
                                bluetoothUuid?.let {
                                    put("bluetoothUuid", it)
                                }
                            }*/
                            /*println("json go to backend --->  $json")

                            val requestBody = json.toString()
                                .toRequestBody("application/json".toMediaTypeOrNull())

                            println("generated code requestBody --->  $requestBody")
                            Log.d("JSON", requestBody.toString())
                            Toast.makeText(context,"$requestBody", Toast.LENGTH_LONG).show()

                            val call = RetrofitClient.instance.generateCode(requestBody)
                            call.enqueue(object : Callback<ResponseBody> {
                                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                    if (response.isSuccessful) {
                                        val result = response.body()?.string()
                                        //responseMessage = result
                                        if (result != null) {
                                            responseList = (responseList + result).toMutableList()
                                        }
                                    } else {
                                        responseMessage = "⚠️ Server Error: ${response.errorBody()?.string()}"
                                    }
                                }

                                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                    responseMessage = "🚫 Network error: ${t.message}"
                                }
                            })*/
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
            /*responseMessage?.let { rawResponse ->
                val formatted = rawResponse
                    .replace("{", "")
                    .replace("}", "")
                    .replace("\"", "")
                    .replace(",", "\n")
                    .replace(":", " : ")
                    .trim()

                /*AlertDialog(
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
                )*/
                // FULL-SCREEN DIM BACKGROUND + CENTERED CARD
                /*Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))        // Dim background
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E2E)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = "Attendance Code Generated",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                "Share this code with your students:",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Response text section
                            Text(
                                text = formatted,
                                color = Color(0xFF64B5F6),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                "Valid for 2–3 minutes",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { responseMessage = null },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(0.5f)
                            ) {
                                Text("OK")
                            }
                        }
                    }
                }*/
                /*Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E2E)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                    ) {

                        // SCROLLABLE CONTENT SECTION
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .heightIn(min = 200.dp, max = 450.dp)   // ADD HEIGHT LIMIT
                                .verticalScroll(rememberScrollState()), // ENABLE SCROLLING
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = "Attendance Code Generated",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                "Share this code with your students:",
                                color = Color.LightGray
                            )

                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = formatted,
                                color = Color(0xFF64B5F6),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )

                            Spacer(Modifier.height(14.dp))

                            Text(
                                "Valid for 2–3 minutes",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(Modifier.height(20.dp))
                        }

                        // OK button (fixed at bottom)
                        Button(
                            onClick = { responseMessage = null },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text("OK")
                        }
                    }
                }*/
            }*/
            //if (responseMessage != null)
            if (responseList.isNotEmpty())
            {

                // ---- Extract values from JSON ----
                /*Log.d("DEBUG_RESPONSE", responseMessage!!)
                val json = JSONObject(responseMessage!!)
                val code = json.getString("code")
                val department = json.getString("department")
                val subject = json.getString("subject")
                val className = json.getString("className")
                val generatedAt = json.getString("generatedAt")
                val expiresAt = json.getString("expiresAt")

                // ---- Parse time + timer ----
                var remainingTime by remember { mutableStateOf(0L) }

                LaunchedEffect(Unit) {
                    val format = SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.getDefault())
                    val genTime = format.parse(generatedAt)?.time ?: 0L
                    val expTime = format.parse(expiresAt)?.time ?: 0L

                    remainingTime = maxOf((expTime - genTime) / 1000,0)
                }

                LaunchedEffect(remainingTime) {
                    while (remainingTime > 0) {
                        delay(1000)
                        remainingTime--
                    }
                }

                val timerText = String.format("%02d:%02d", remainingTime / 60, remainingTime % 60)

                // ---- UI POPUP ----
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {

                    Card(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {

                        Column(Modifier.padding(20.dp)) {

                            // ---- TOP BAR WITH CLOSE BUTTON ----
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Attendance Code",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(onClick = {

                                    // ---- API CALL BEFORE CLOSING ----
                                    val jsonReq = JSONObject().apply {
                                        put("teacherId", SessionManager.teacherId)
                                        put("department", department)
                                        put("subject", subject)
                                        put("className", className)
                                    }

                                    val body = jsonReq.toString()
                                        .toRequestBody("application/json".toMediaTypeOrNull())

                                    RetrofitClient.instance.closeCode(body)
                                        .enqueue(object : Callback<ResponseBody> {
                                            override fun onResponse(
                                                call: Call<ResponseBody>,
                                                response: Response<ResponseBody>
                                            ) {
                                                val r = response.body()?.string()
                                                if (r != null && JSONObject(r).getBoolean("success")) {
                                                    responseMessage = null
                                                }
                                            }

                                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {}
                                        })

                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // ---- CODE IN LARGE SIZE ----
                            Text(
                                text = code,
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1976D2)
                                )
                            )

                            Spacer(Modifier.height(16.dp))

                            // ---- DETAILS ----
                            Text("Department: $department", fontWeight = FontWeight.Medium)
                            Text("Subject: $subject", fontWeight = FontWeight.Medium)

                            Spacer(Modifier.height(20.dp))

                            // ---- TIMER ----
                            Text(
                                text = "Expires in: $timerText",
                                color = if (remainingTime < 20) Color.Red else Color(0xFF388E3C),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }*/
                LazyColumn {
                    items (responseList) { res -> // error occur

                        CompactCodeCard(
                            //response = responseMessage!!,
                            response = res, // error occur
                            onClose = {

                                // CALL Close API
                                val jsonReq = JSONObject().apply {
                                    val obj = JSONObject(res)
                                    put("teacherId", SessionManager.teacherId)
                                    //put("department", JSONObject(responseMessage!!).getString("department"))
                                    //put("subject", JSONObject(responseMessage!!).getString("subject"))
                                    //put("className", JSONObject(responseMessage!!).getString("className"))
                                    put("department", obj.getString("department"))
                                    put("subject", obj.getString("subject"))
                                    put("className", obj.getString("className"))
                                }

                                val body = jsonReq.toString()
                                    .toRequestBody("application/json".toMediaTypeOrNull())

                                RetrofitClient.instance.closeCode(body)
                                    .enqueue(object : Callback<ResponseBody> {
                                        override fun onResponse(
                                            call: Call<ResponseBody>,
                                            response: Response<ResponseBody>
                                        ) {
                                            val r = response.body()?.string()
                                            if (r != null && JSONObject(r).getBoolean("success")) {
                                                //responseMessage = null
                                                responseList = responseList.toMutableList().also { it.remove(res) }
                                            }
                                        }
                                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {}
                                    })
                            }
                        )
                    }
                }
            }

        }
    }
}

@Composable
fun CompactCodeCard(response: String, onClose: () -> Unit) {
    val json = JSONObject(response)
    val code = json.getString("code")
    val department = json.getString("department")
    val subject = json.getString("subject")
    val className = json.getString("className")
    val generatedAt = json.getString("generatedAt")
    val expiresAt = json.getString("expiresAt")

    var remainingTime by remember { mutableStateOf(0L) }

    // TIMER CALCULATION
    LaunchedEffect(Unit) {
        val format = SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.getDefault())
        val genTime = format.parse(generatedAt)?.time ?: 0L
        val expTime = format.parse(expiresAt)?.time ?: 0L
        remainingTime = maxOf((expTime - genTime) / 1000, 0)
    }

    LaunchedEffect(remainingTime) {
        while (remainingTime > 0) {
            delay(1000)
            remainingTime--
            Log.d("Remaining time","$remainingTime")
        }
    }

    val timerText = String.format("%02d:%02d", remainingTime / 60, remainingTime % 60)

    // ---- COMPACT CARD UI ----
    Card(
        modifier = Modifier
            .padding(12.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {

        Column(Modifier.padding(16.dp)) {

            // TOP ROW
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "$code",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )

                )

                /*IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }*/
                if (remainingTime.toInt() != 0) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.padding(end = 6.dp).size(30.dp) // Button Size
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close_circle),
                            contentDescription = "Close",
                            tint = Color.Unspecified,   // IMPORTANT: do not recolor XML
                            modifier = Modifier.size(50.dp) // Icon Size
                        )
                    }
                }
                else {
                    Text(
                        text = "Expired",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFFFF0000)
                        )

                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            // BOTTOM ROW
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {
                    Text("$department | $subject", fontWeight = FontWeight.Medium)
                    //Text("$subject", fontWeight = FontWeight.Medium)
                }

                Text(
                    text = timerText,
                    color = if (remainingTime < 20) Color.Red else Color(0xFF388E3C),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
