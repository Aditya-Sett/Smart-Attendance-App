package com.mckv.attendance.ui.screens

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.drawscope.EmptyCanvas.drawCircle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.mckv.attendance.data.local.SessionManager
import com.mckv.attendance.data.remote.RetrofitClient
import com.mckv.attendance.ui.components.common.CommonTopBar
import com.mckv.attendance.utils.ensureBluetoothPermissions
import com.mckv.attendance.utils.getCurrentLocation
import com.mckv.attendance.utils.getWifiFingerPrint
import com.mckv.attendance.utils.interactionDetection
import com.mckv.attendance.utils.scanForTeacherUuid
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// 🔹 Main Home Screen
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val activity = context as Activity
    val studentId = SessionManager.studentId ?: "Unknown"
    val department = SessionManager.department ?: "Unknown"
    val admissionYear = SessionManager.admissionYear ?: "Unknown"
    val lifecycleOwner = LocalLifecycleOwner.current

    var activeCode by remember { mutableStateOf<String?>(null) }
    var activeSubject by remember { mutableStateOf<String?>(null) }
    var expiresAt by remember { mutableStateOf<Long?>(null) }

    var showDialog by remember { mutableStateOf(false) }
    var inputCode by remember { mutableStateOf("") }
    var responseMessage by remember { mutableStateOf<String?>(null) }

    // Bluetooth states
    var bluetoothEnabled by remember { mutableStateOf(false) }
    var bluetoothChecking by remember { mutableStateOf(true) }
    var bluetoothSupported by remember { mutableStateOf(true) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 🔹 Function to check Bluetooth status
    val checkBluetoothStatus = {
        if (ensureBluetoothPermissions(activity)) {
            val bluetoothAdapter: BluetoothAdapter? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
                bluetoothManager?.adapter
            } else {
                BluetoothAdapter.getDefaultAdapter()
            }

            if (bluetoothAdapter == null) {
                // Device doesn't support Bluetooth
                bluetoothSupported = false
                bluetoothEnabled = false
            } else {
                bluetoothSupported = true
                bluetoothEnabled = bluetoothAdapter.isEnabled
            }
            bluetoothChecking = false
            Log.d("BluetoothStatus", "Bluetooth enabled: $bluetoothEnabled, Supported: $bluetoothSupported")
        }
    }

    // 🔹 Broadcast Receiver to monitor Bluetooth state changes
    val bluetoothStateReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR
                        )
                        when (state) {
                            BluetoothAdapter.STATE_ON -> {
                                Log.d("BluetoothState", "Bluetooth turned ON")
                                bluetoothEnabled = true
                                bluetoothChecking = false
                            }
                            BluetoothAdapter.STATE_OFF -> {
                                Log.d("BluetoothState", "Bluetooth turned OFF")
                                bluetoothEnabled = false
                                bluetoothChecking = false
                            }
                            BluetoothAdapter.STATE_TURNING_ON -> {
                                Log.d("BluetoothState", "Bluetooth turning ON...")
                                bluetoothChecking = true
                            }
                            BluetoothAdapter.STATE_TURNING_OFF -> {
                                Log.d("BluetoothState", "Bluetooth turning OFF...")
                                bluetoothChecking = true
                            }
                        }
                    }
                }
            }
        }
    }

    // 🔹 Register and unregister Bluetooth receiver
    DisposableEffect(lifecycleOwner) {
        val intentFilter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        context.registerReceiver(bluetoothStateReceiver, intentFilter)

        // Initial check
        checkBluetoothStatus()

        onDispose {
            context.unregisterReceiver(bluetoothStateReceiver)
        }
    }

    // 🔹 Poll backend every 2 sec for active code (only if Bluetooth is enabled)
    LaunchedEffect(bluetoothEnabled) {
        if (bluetoothEnabled) {
            while (true) {
                getCurrentLocation(context) { lat, lon ->
                    checkForActiveCode(
                        context = context,
                        department = department,
                        admissionYear = admissionYear,
                        lat = lat,
                        lon = lon,
                        onFound = { code, subject, expiry ->
                            if (code != SessionManager.lastCodeSubmitted) {
                                activeCode = code
                                activeSubject = subject
                                expiresAt = expiry
                                showDialog = true
                            }
                        },
                        onError = { msg -> Log.e("DEBUG", "❌ $msg") }
                    )
                }
                delay(2000L) // every 2 sec
            }
        }
    }

    // 🔹 Function to request Bluetooth enable
    val requestBluetoothEnable = {
        if (!bluetoothEnabled) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivityForResult(intent, 1001)
        }
    }

    // 🔹 Code entry dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                inputCode = ""
            },
            confirmButton = {
                Button(onClick = {
                    if (inputCode.length != 4) {
                        Toast.makeText(context, "Enter a valid 4-digit code", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    submitAttendance(
                        context = context,
                        studentId = studentId,
                        department = department,
                        inputCode = inputCode,
                        activeCode = activeCode,
                        onSuccess = {
                            SessionManager.lastCodeSubmitted = activeCode
                            responseMessage = "✅ Attendance marked"
                        },
                        onFailure = { msg -> responseMessage = msg }
                    )

                    showDialog = false
                    inputCode = ""
                }) {
                    Text("Submit")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showDialog = false
                    inputCode = ""
                }) { Text("Cancel") }
            },
            title = { Text("Attendance for $activeSubject") },
            text = {
                Column {
                    expiresAt?.let {
                        val minutesLeft = ((it - System.currentTimeMillis()) / 60000).coerceAtLeast(0)
                        Text("Expires in $minutesLeft min")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { if (it.length <= 4) inputCode = it },
                        label = { Text("Enter 4-digit Code") },
                        singleLine = true
                    )
                }
            }
        )
    }

    // 🔹 Bluetooth Status Indicator
    @Composable
    fun BluetoothStatusIndicator() {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = when {
                            bluetoothChecking -> Color.Yellow
                            bluetoothEnabled -> Color.Green
                            else -> Color.Red
                        },
                        shape = RoundedCornerShape(50)
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when {
                    bluetoothChecking -> "Bluetooth: Checking..."
                    !bluetoothSupported -> "Bluetooth: Not Supported"
                    bluetoothEnabled -> "Bluetooth: ON"
                    else -> "Bluetooth: OFF"
                },
                color = Color.White,
                fontSize = 12.sp
            )

            // Add refresh button if Bluetooth is off
            if (!bluetoothChecking && !bluetoothEnabled && bluetoothSupported) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        bluetoothChecking = true
                        checkBluetoothStatus()
                    },
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Bluetooth Status",
                        tint = Color.White
                    )
                }
            }
        }
    }

    // 🔹 Main UI
    Scaffold(
        modifier = Modifier.interactionDetection(),
        topBar = {
            Column {
                CommonTopBar(
                    title = "Smart Attendance",
                    navController = navController
                )

                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2196F3))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left side: Welcome and Department
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Welcome, $studentId", color = Color.White)
                            Text("Department: $department", color = Color.White)
                        }

                        // Right side: Bluetooth indicator
                        BluetoothStatusIndicator()
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Show loading or Bluetooth warning
            when {
                bluetoothChecking -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Checking Bluetooth status...")
                    }
                }

                !bluetoothSupported -> {
                    // Bluetooth not supported
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFE0E0)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "⚠️ Bluetooth Not Supported",
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your device does not support Bluetooth",
                                color = Color.Red
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Attendance marking will not work",
                                color = Color.Red,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                !bluetoothEnabled -> {
                    // Bluetooth is disabled - show prominent warning
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3CD)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "⚠️ Bluetooth is required for attendance marking",
                                color = Color(0xFF856404),
                                //fontWeight = FontWeight.Normal
                                fontSize = 12.sp
                            )
                            /*Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Bluetooth is required for attendance marking",
                                color = Color(0xFF856404)
                            )*/
                            Spacer(modifier = Modifier.height(5.dp))
                            Button(
                                onClick = requestBluetoothEnable,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF856404)
                                ),
                                enabled = !bluetoothChecking
                            ) {
                                if (bluetoothChecking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    //Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text("Enable Bluetooth")
                            }
                            /*Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { checkBluetoothStatus() },
                                enabled = !bluetoothChecking
                            ) {
                                Text("Check Again")
                            }*/
                        }
                    }
                }

                else -> {
                    // Bluetooth is enabled
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E8)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✓ Ready for Attendance",
                                    color = Color(0xFF155724),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { checkBluetoothStatus() },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh Bluetooth Status",
                                        tint = Color.White
                                    )
                                }
                            }
                            Text(
                                text = "Bluetooth is enabled. You will be notified when a teacher starts attendance.",
                                color = Color(0xFF155724),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Main buttons (always enabled, but show warning if Bluetooth is off)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { navController.navigate("schedule") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !bluetoothChecking
                ) {
                    Text("📅 View Schedule")
                }

                Button(
                    onClick = { navController.navigate("attendance_summary") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !bluetoothChecking
                ) {
                    Text("📊 Attendance History")
                }

                // Warning if Bluetooth is off but user can still access other features
                if (!bluetoothEnabled && !bluetoothChecking && bluetoothSupported) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF0F0F0)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "ℹ️ Note",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "You can still view schedule and attendance history, but attendance marking requires Bluetooth.",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // 🔹 Show snackbar messages
    LaunchedEffect(responseMessage) {
        responseMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            responseMessage = null
        }
    }
}

// 🔹 Check backend for active code
fun checkForActiveCode(
    context: Context,
    department: String,
    admissionYear: String,
    lat: Double,
    lon: Double,
    onFound: (String, String, Long) -> Unit,
    onError: (String) -> Unit
) {
    val json = JSONObject().apply {
        put("department", department)
        put("admissionYear", admissionYear)
    }

    val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

    Log.d("GAC Checking", "🔎 Sending -> Dept=$department, AdmissionYear=$admissionYear")

    RetrofitClient.instance.getLatestCode(requestBody).enqueue(object : Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            if (response.isSuccessful) {
                Log.d("GAC Checking", "✅ Code fetched successfully")
                val bodyString = response.body()?.string() ?: return
                val result = JSONObject(bodyString)
                val code = result.optString("code")
                val subject = result.optString("subject")
                val expiresAt = result.optLong("expiresAt")
                val bluetoothUuid = result.optString("bluetoothUuid")

                scanForTeacherUuid(context, bluetoothUuid) { match ->
                    if (match) {
                        Log.d("BLE", "✅ Teacher is nearby! UUID matched.")
                        onFound(code, subject, expiresAt)
                    } else {
                        Log.d("BLE", "❌ Teacher NOT nearby. UUID mismatch.")
                        onError("Teacher not nearby (BLE mismatch)")
                    }
                }
            } else onError("Server returned ${response.code()}")
        }
        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            onError("Network error: ${t.message}")
        }
    })
}

// 🔹 Submit attendance with WiFi fingerprint
fun submitAttendance(
    context: Context,
    studentId: String,
    department: String,
    inputCode: String,
    activeCode: String?,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    val wifiFingerprint = getWifiFingerPrint(context)

    val json = JSONObject().apply {
        put("studentId", studentId)
        put("department", department)
        put("code", inputCode)
        put("wifiFingerprint", wifiFingerprint)
    }

    val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

    RetrofitClient.instance.submitAttendanceCode(requestBody)
        .enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val result = JSONObject(response.body()?.string() ?: "{}")
                    Log.d("Result", result.toString())
                    if (result.optBoolean("success")) onSuccess()
                    else onFailure("❌ Invalid code or WiFi mismatch")
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown Error"
                    onFailure("⚠️ Code invalid or expired $errorBody")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                onFailure("🚫 Network error: ${t.message}")
            }
        })
}