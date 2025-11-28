package com.mckv.attendance.ui.screens

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mckv.attendance.data.local.SessionManager
import com.mckv.attendance.data.remote.RetrofitClient
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import com.mckv.attendance.utils.getCurrentLocation
import com.mckv.attendance.utils.getWifiFingerPrint

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.OutlinedButton
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.sp
import com.mckv.attendance.data.local.TokenExpiryManager
import com.mckv.attendance.ui.components.common.CommonTopBar
import com.mckv.attendance.utils.UserInteractionHandler
import com.mckv.attendance.utils.ensureBluetoothPermissions
import com.mckv.attendance.utils.interactionDetection
import com.mckv.attendance.utils.logoutUser
import com.mckv.attendance.utils.scanForTeacherUuid

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

    var activeCode by remember { mutableStateOf<String?>(null) }
    var activeSubject by remember { mutableStateOf<String?>(null) }
    var expiresAt by remember { mutableStateOf<Long?>(null) }

    var showDialog by remember { mutableStateOf(false) }
    var inputCode by remember { mutableStateOf("") }
    var responseMessage by remember { mutableStateOf<String?>(null) }

    // 🔹 Dropdown menu state
    var showDropdownMenu by remember { mutableStateOf(false) }


    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // State for showing dialog
    var showExpiryDialog by remember { mutableStateOf(false) }

    // 🔹 Add User Interaction Handler for Token Expiry Detection
    UserInteractionHandler(navController = navController)

    // 🔹 Show snackbar messages
    LaunchedEffect(responseMessage) {
        responseMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            responseMessage = null
        }
    }

    // Checking whether bluetooth on or off ,, if off then request user to turn on
    if (!ensureBluetoothPermissions(activity)) {
        return//@Button     // wait for user to grant permission
    }
    val bluetoothAdapter: BluetoothAdapter? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        bluetoothManager.adapter
    } else {
        // Android < 12
        BluetoothAdapter.getDefaultAdapter()
    }

    if (bluetoothAdapter == null) {
        println ("Device does NOT support Bluetooth")
        return//@Button
    } else {
        if (bluetoothAdapter.isEnabled) {
            // Bluetooth is ON
            println("Bluetooth is on")
        } else {
            // Bluetooth is OFF
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivityForResult(intent, 1001)
            return//@Button
        }
    }

    // 🔹 Poll backend every 2 sec for active code
    LaunchedEffect(Unit) {
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



//    // Function to check and show dialog
//    val checkTokenAndShowDialog = {
//        if (TokenExpiryManager.isTokenExpired() && !showExpiryDialog) {
//            showExpiryDialog = true
//            TokenExpiryManager.setDialogShowing(true)
//        }
//    }
//
//    // Show dialog if token is expired
//    if (showExpiryDialog) {
//        AlertDialog(
//            onDismissRequest = { /* Don't allow dismiss */ },
//            title = {
//                Text(
//                    text = "Session Expired",
//                    color = MaterialTheme.colorScheme.error
//                )
//            },
//            text = {
//                Text("Your session has expired. Please login again.")
//            },
//            confirmButton = {
//                Button(
//                    onClick = {
//                        showExpiryDialog = false
//                        TokenExpiryManager.setDialogShowing(false)
//                        logoutUser(context, navController)
//                    }
//                ) {
//                    Text("OK, Login Again")
//                }
//            }
//        )
//    }

    // 🔹 Main UI
    Scaffold(

        modifier = Modifier.interactionDetection(),
        topBar = {
            Column {

                CommonTopBar(
                    title = "Smart Attendance",
                    navController = navController
                )

//                TopAppBar(
//                    title = { Text("Smart Attendance") },
//                    actions = {
//                        // 🔹 Profile Icon with Dropdown Menu
//                        Box {
//                            IconButton(
//                                onClick = { showDropdownMenu = true }
//                            ) {
//                                Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
//                            }
//
//                            // 🔹 Dropdown Menu
//                            // 🔹 Dropdown Menu with Attractive Styling
//                            DropdownMenu(
//                                expanded = showDropdownMenu,
//                                onDismissRequest = { showDropdownMenu = false },
//                                modifier = Modifier
//                                    .background(
//                                        color = Color(0xFF1976D2), // Dark blue background
//                                        shape = RoundedCornerShape(2.dp)
//                                    )
//                            ) {
//                                // 🔹 Profile Info Item
//                                DropdownMenuItem(
//                                    text = {
//                                        Text(
//                                            text = "Profile",
//                                            color = Color.White,
//                                            fontSize = 16.sp,
//                                            fontWeight = FontWeight.Medium,
//                                            fontFamily = FontFamily.SansSerif,
//                                            modifier = Modifier.padding(vertical = 4.dp)
//                                        )
//                                    },
//                                    onClick = {
//                                        showDropdownMenu = false
//                                        // You can add navigation to profile screen here
//                                        Toast.makeText(context, "Profile Info", Toast.LENGTH_SHORT).show()
//                                    },
//                                    leadingIcon = {
//                                        Icon(
//                                            Icons.Default.Person,
//                                            contentDescription = "Profile",
//                                            tint = Color(0xFF64FFDA) // Teal accent color
//                                        )
//                                    },
//                                    colors =  MenuItemColors(
//                                        textColor = Color.White,
//                                        disabledTextColor = Color.Gray,
//                                        leadingIconColor = Color(0xFF64FFDA), // Correct parameter name
//                                        disabledLeadingIconColor = Color.Gray,
//                                        trailingIconColor = Color.White,
//                                        disabledTrailingIconColor = Color.Gray
//                                    )
//                                )
//
//                                // 🔹 Attractive separator
//                                Spacer(modifier = Modifier
//                                    .fillMaxWidth()
//                                    .height(1.dp)
//                                    .padding(horizontal = 8.dp)
//                                    .background(Color(0xFF64FFDA).copy(alpha = 0.3f))
//                                )
//
//                                // 🔹 Logout Item
//                                DropdownMenuItem(
//                                    text = {
//                                        Text(
//                                            text = "Logout",
//                                            color = Color(0xFFFF6B6B), // Red color for logout
//                                            fontSize = 16.sp,
//                                            fontWeight = FontWeight.Medium,
//                                            fontFamily = FontFamily.SansSerif,
//                                            modifier = Modifier.padding(vertical = 4.dp)
//                                        )
//                                    },
//                                    onClick = {
//                                        showDropdownMenu = false
//                                        logoutUser(context, navController)
//                                    },
//                                    leadingIcon = {
//                                        Icon(
//                                            Icons.AutoMirrored.Filled.ExitToApp,
//                                            contentDescription = "Logout",
//                                            tint = Color(0xFFFF6B6B) // Red accent color
//                                        )
//                                    },
//                                    colors = MenuItemColors(
//                                        textColor = Color(0xFFFF6B6B),
//                                        disabledTextColor = Color.Gray,
//                                        leadingIconColor = Color(0xFFFF6B6B), // Correct parameter name
//                                        disabledLeadingIconColor = Color.Gray,
//                                        trailingIconColor = Color.White,
//                                        disabledTrailingIconColor = Color.Gray
//                                    )
//                                )
//                            }
//                        }
//                    },
//                    colors = TopAppBarDefaults.topAppBarColors(
//                        containerColor = Color(0xFF1976D2),
//                        titleContentColor = Color.White
//                    )
//                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2196F3))
                        .padding(16.dp)
                ) {
                    Text("Welcome, $studentId", color = Color.White)
                    Text("Department: $department", color = Color.White)
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
            Button(
                onClick = { navController.navigate("schedule") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("📅 View Schedule") }

            Button(
                onClick = { navController.navigate("attendance_summary") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("📊 Attendance History") }
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
        //put("studentLat", lat)
        //put("studentLon", lon)
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
                /*if (result.optBoolean("active") && code.isNotBlank()) {
                    onFound(code, subject, expiresAt)
                }*/
                scanForTeacherUuid(context, bluetoothUuid) { match ->
                    if (match) {
                        Log.d("BLE", "✅ Teacher is nearby! UUID matched.")
                        onFound(code, subject, expiresAt)   // allow dialog to show
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
    context: android.content.Context,
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
        put("wifiFingerprint", wifiFingerprint)  // 🔹 only sent, backend checks
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
                    val errorBody = response.errorBody()?.string()?: "Unknown Error"
                    onFailure("⚠️ Code invalid or expired $errorBody")
                    /*val bodyStringgg = response.body()?.string()
                    if (bodyStringgg != null) {
                        Log.d("bodyStringgg", bodyStringgg)
                        val json_bodystring = JSONObject(bodyStringgg)
                        val message = json_bodystring.optString("message")
                        onFailure("⚠️ $message")
                    }
                    else {
                        Log.d("bodyStringgg", "bodyStringgg is null")
                    }
                    /*if (bodyStringgg != null) {
                        val json_bodystring = JSONObject(bodyStringgg)
                        val message = json_bodystring.optString("message")
                        //Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }*/

                */}
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                onFailure("🚫 Network error: ${t.message}")
            }
        })
}
