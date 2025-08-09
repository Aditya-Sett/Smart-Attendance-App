package com.mckv.attendance

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mckv.attendance.model.RetrofitClient
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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

    // Auto-check for active code every 10 seconds
    Log.d("DEBUG", "🧠 department in SessionManager: ${SessionManager.department}")
    LaunchedEffect(Unit) {
        while (true) {
            Log.d("DEBUG", "🧠 department in SessionManager: ${SessionManager.department}")
            Log.d("DEBUG", "Checking for code...")
            Log.d("DEBUG", "📡 Sending request to getLatestCode($department)")

            val call = RetrofitClient.instance.getLatestCode(department)
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
            })

            delay(2000) // Check every 2s
        }
    }

    // Dialog for submitting code
    if (showDialog && activeCode != null) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                Button(onClick = {
                    if (inputCode.length != 4) {
                        Toast.makeText(context, "Enter a valid 4-digit code", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    Log.d("DEBUG", "📤 Submitting studentId=$studentId, department=$department, code=$inputCode")

                    val json = JSONObject().apply {
                        put("studentId", studentId)
                        put("code", inputCode)
                        put("department", department)
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
                }) {
                    Text("Submit")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showDialog = false
                }) {
                    Text("Cancel")
                }
            },
            title = { Text("📥 Enter Attendance Code") },
            text = {
                Column {
                    Text("An active attendance code is available for your class.")
                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { inputCode = it },
                        label = { Text("Attendance Code") }
                    )
                }
            }
        )
    }

    // Main content
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Smart Attendance App") })
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = { navController.navigate("schedule") }) {
                    Text("📅 Get Schedule")
                }
                Spacer(modifier = Modifier.height(16.dp)) // optional spacing between buttons
                Button(
                    onClick = {
                        navController.navigate("attendance_summary")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                ) {
                    Text(text = "📊 Attendance History")
                }

                responseMessage?.let {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(it)
                }
            }
        }
    }
}
