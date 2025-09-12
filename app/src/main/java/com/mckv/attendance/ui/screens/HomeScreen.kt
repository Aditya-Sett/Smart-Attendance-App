package com.mckv.attendance.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.OutlinedButton
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val studentId = SessionManager.studentId ?: "Unknown"
    val department = SessionManager.department ?: "Unknown"
    var activeCode by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var inputCode by remember { mutableStateOf("") }
    var responseMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(responseMessage) {
        responseMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
            }
            responseMessage = null // reset after showing
        }
    }

    // Auto-check for active code every 10 seconds
    Log.d("DEBUG", "🧠 department in SessionManager: ${SessionManager.department}")
    LaunchedEffect(Unit) {
        while (true) {
            Log.d("DEBUG", "🧠 department in SessionManager: ${SessionManager.department}")
            Log.d("DEBUG", "Checking for code...")
            Log.d("DEBUG", "📡 Sending request to getLatestCode($department)")

            getCurrentLocation(context) {lat, lon ->
                val json = JSONObject().apply {
                    put("department", department)
                    put("studentLat", lat)
                    put("studentLon", lon)
                }
                val requestBody = json.toString()
                    .toRequestBody("application/json".toMediaTypeOrNull())

                val call = RetrofitClient.instance.getLatestCode(requestBody)
                call.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        Log.d("DEBUG", "✅ Received response")
                        if (response.isSuccessful) {
                            val bodyString = response.body()?.string() ?: ""
                            Log.d("DEBUG", "Raw body: $bodyString")

                            if (bodyString.isNotBlank()) {
                                try {
                                    val result = JSONObject(bodyString)
                                    val code = result.optString("code")
                                    val active = result.optBoolean("active")

                                    if (active && code.isNotBlank() && code != SessionManager.lastCodeSubmitted) {
                                        activeCode = code
                                        showDialog = true
                                    }
                                } catch (e: Exception) {
                                    Log.e("DEBUG", "❌ JSON parsing failed: ${e.message}")
                                }
                            }
                        } else {
                            Log.e("DEBUG", "❌ Unsuccessful response: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e("DEBUG", "🚫 Network error: ${t.message}")
                    }
                })
            }

            /*val call = RetrofitClient.instance.getLatestCode(department)
            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    Log.d("DEBUG", "✅ Received response")
                    if (response.isSuccessful) {
                        val bodyString = response.body()?.string() ?: ""
                        Log.d("DEBUG", "Raw body: $bodyString")

                        // ✅ Safe JSON parsing block
                        if (bodyString.isNotBlank()) {
                            try {
                                val result = JSONObject(bodyString)
                                val code = result.optString("code")
                                val active = result.optBoolean("active")

                                Log.d("DEBUG", "Active: $active, Code: $code, Last: ${SessionManager.lastCodeSubmitted}")

                                if (active && code.isNotBlank() && code != SessionManager.lastCodeSubmitted) {
                                    activeCode = code
                                    showDialog = true
                                }
                            } catch (e: Exception) {
                                Log.e("DEBUG", "❌ JSON parsing failed: ${e.message}")
                            }
                        } else {
                            Log.e("DEBUG", "❌ Empty response body — skipping JSON parsing")
                        }
                    } else {
                        Log.e("DEBUG", "❌ Unsuccessful response: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("DEBUG", "🚫 Network error: ${t.message}")
                }
            })*/

            delay(2000) // Check every 2s
        }
    }

    // Dialog for submitting code
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                Button(onClick = {
                    if (inputCode.length != 4) {
                        Toast.makeText(context, "Enter a valid 4-digit code", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    Log.d("DEBUG", "📤 Submitting studentId=$studentId, department=$department, code=$inputCode")

                    getCurrentLocation(context) {lat, lon ->
                        val json = JSONObject().apply {
                            put("studentId", studentId)
                            put("department", department)
                            put("code", inputCode)
                            put("studentLat", lat)
                            put("studentLon", lon)
                        }

                        val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

                        RetrofitClient.instance.submitAttendanceCode(requestBody)
                            .enqueue(object : Callback<ResponseBody> {
                                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                    Log.d("DEBUG", "🔄 Submit response code: ${response.code()}")
                                    if (response.isSuccessful) {
                                        val bodyString = response.body()?.string() ?: ""
                                        Log.d("DEBUG", "Raw response from backend: $bodyString")

                                        try {
                                            val result = JSONObject(bodyString)
                                            val success = result.optBoolean("success")
                                            Log.d("DEBUG", "✅ Parsed success: $success")

                                            if (success) {
                                                Log.d("DEBUG", "🔑 activeCode = $activeCode")
                                                SessionManager.lastCodeSubmitted = activeCode
                                                Log.d("DEBUG", "💾 Saved lastCodeSubmitted: $activeCode")
                                                responseMessage = "✅ Attendance marked"
                                            } else {
                                                responseMessage = "❌ Code invalid"
                                            }
                                        } catch (e: Exception) {
                                            Log.e("DEBUG", "❌ JSON parse error: ${e.message}")
                                            responseMessage = "⚠️ Response error"
                                        }
                                    } else {
                                        Log.e("DEBUG", "❌ Submission failed with code: ${response.code()}")
                                        responseMessage = "❌ Code invalid or expired"
                                    }
                                    showDialog = false

                                }

                                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                    Log.e("DEBUG", "🚫 Network error on submit: ${t.message}")
                                    responseMessage = "🚫 Network error: ${t.message}"
                                    showDialog = false
                                }
                            })
                    }

                }) {
                    Text("Submit")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Enter Attendance Code") },
            text = {
                Column {
                    Text("Enter the 4-digit attendance code shared by your teacher.")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { if (it.length <= 4) inputCode = it },
                        label = { Text("Attendance Code") },
                        singleLine = true
                    )
                }
            },
            shape = MaterialTheme.shapes.large
        )
    }

    // Main content
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Smart Attendance") },
                    navigationIcon = {
                        IconButton(onClick = { /* TODO: open drawer */ }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Profile */ }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1976D2), // bluish shade
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )

                // Student info bar under AppBar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2196F3)) // lighter blue
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Welcome, $studentId",
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                    )
                    Text(
                        text = "Department: $department",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                /*Text(
                    text = "Welcome, $studentId",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Department: $department",
                    style = MaterialTheme.typography.bodyMedium
                )*/

                Button(
                    onClick = { navController.navigate("schedule") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                ) {
                    Text("📅 View Schedule")
                }

                Button(
                    onClick = { navController.navigate("attendance_summary") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                ) {
                    Text("📊 Attendance History")
                }
            }
        }
    }

}
