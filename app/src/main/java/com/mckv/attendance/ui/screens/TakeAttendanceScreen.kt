package com.mckv.attendance.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.mckv.attendance.data.local.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import com.mckv.attendance.data.remote.RetrofitClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import org.json.JSONObject

// Material3
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults

// Compose UI
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.School


import com.mckv.attendance.utils.getCurrentLocation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeAttendanceScreen(navController: NavHostController) {
    val context = LocalContext.current
    val teacherId = SessionManager.teacherId ?: "Unknown"

    var department by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var classroom by remember { mutableStateOf("") }
    var responseMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teacher Panel") },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Add drawer later */ }) {
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

                    OutlinedTextField(
                        value = classroom,
                        onValueChange = { classroom = it },
                        label = { Text("Classroom Number") },
                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (teacherId == "Unknown" || department.isBlank() || subject.isBlank() || classroom.isBlank()) {
                                Toast.makeText(context, "⚠ Fill all fields", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val json = JSONObject().apply {
                                put("teacherId", teacherId)
                                put("department", department)
                                put("subject", subject)
                                put("classroom", classroom)  // ✅ instead of lat/lon
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
