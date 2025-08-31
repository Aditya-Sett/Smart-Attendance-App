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


@Composable
fun TakeAttendanceScreen(navController: NavHostController) {
    val context = LocalContext.current
    val teacherId = SessionManager.teacherId ?: "Unknown"

    var department by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var responseMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("👨‍🏫 Teacher ID: $teacherId")

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = department,
            onValueChange = { department = it },
            label = { Text("Department") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (teacherId == "Unknown" || department.isBlank() || subject.isBlank()) {
                    Toast.makeText(context, "⚠ Fill all fields", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val json = JSONObject().apply {
                    put("teacherId", teacherId)
                    put("department", department)
                    put("subject", subject)
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
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🎯 Generate Code")
        }

        /*responseMessage?.let {
            Spacer(modifier = Modifier.height(20.dp))
            Text("📢 Response from server:\n$it")
        }*/
        val formatted = responseMessage
            ?.replace("{", "")
            ?.replace("}", "")
            ?.replace("\"", "")
            ?.replace(",", "\n")
            ?.replace(":", " : ")
            ?.replace("T", " T ")
            ?.trim()
        formatted?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text("📢 Response from server:\n$it")
        }

    }
}
