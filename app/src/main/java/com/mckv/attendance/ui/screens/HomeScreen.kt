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
    var activeSubject by remember { mutableStateOf<String?>(null) }
    var activeClassroom by remember { mutableStateOf<String?>(null) }
    var expiresAt by remember { mutableStateOf<Long?>(null) }

    var showDialog by remember { mutableStateOf(false) }
    var inputCode by remember { mutableStateOf("") }
    var responseMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 🔹 Show snackbar messages
    LaunchedEffect(responseMessage) {
        responseMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            responseMessage = null
        }
    }

    // 🔹 Poll backend every 10 sec
    LaunchedEffect(Unit) {
        while (true) {
            getCurrentLocation(context) { lat, lon ->
                checkForActiveCode(
                    department = department,
                    lat = lat,
                    lon = lon,
                    onFound = { code, subject, classroom, expiry ->
                        if (code != SessionManager.lastCodeSubmitted) {
                            activeCode = code
                            activeSubject = subject
                            activeClassroom = classroom
                            expiresAt = expiry
                            showDialog = true
                        }
                    },
                    onError = { msg ->
                        Log.e("DEBUG", "❌ $msg")
                    }
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
                    getCurrentLocation(context) { lat, lon ->
                        submitAttendance(
                            studentId = studentId,
                            department = department,
                            inputCode = inputCode,
                            lat = lat,
                            lon = lon,
                            activeCode = activeCode,
                            onSuccess = {
                                SessionManager.lastCodeSubmitted = activeCode
                                responseMessage = "✅ Attendance marked"
                            },
                            onFailure = { msg -> responseMessage = msg }
                        )
                        showDialog = false
                        inputCode = ""
                    }
                }) { Text("Submit") }
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
                    Text("Classroom: $activeClassroom")
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

    // 🔹 Main UI
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Smart Attendance") },
                    actions = {
                        IconButton(onClick = { /* Profile */ }) {
                            Icon(Icons.Default.AccountCircle, null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1976D2),
                        titleContentColor = Color.White
                    )
                )
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

fun checkForActiveCode(
    department: String,
    lat: Double,
    lon: Double,
    onFound: (String, String, String, Long) -> Unit,
    onError: (String) -> Unit
) {
    val json = JSONObject().apply {
        put("department", department)
        put("studentLat", lat)
        put("studentLon", lon)
    }
    val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

    RetrofitClient.instance.getLatestCode(requestBody).enqueue(object : Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            if (response.isSuccessful) {
                val bodyString = response.body()?.string() ?: return
                val result = JSONObject(bodyString)
                val code = result.optString("code")
                val subject = result.optString("subject")
                val classroom = result.optString("classroom")
                val expiresAt = result.optLong("expiresAt")
                if (result.optBoolean("active") && code.isNotBlank()) {
                    onFound(code, subject, classroom, expiresAt)
                }
            } else onError("Server returned ${response.code()}")
        }
        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            onError("Network error: ${t.message}")
        }
    })
}

fun submitAttendance(
    studentId: String,
    department: String,
    inputCode: String,
    lat: Double,
    lon: Double,
    activeCode: String?,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
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
                if (response.isSuccessful) {
                    val result = JSONObject(response.body()?.string() ?: "{}")
                    if (result.optBoolean("success")) onSuccess()
                    else onFailure("Invalid code")
                } else onFailure("Code invalid or expired")
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                onFailure("Network error: ${t.message}")
            }
        })
}
