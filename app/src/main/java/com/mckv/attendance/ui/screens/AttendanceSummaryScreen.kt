package com.mckv.attendance.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mckv.attendance.data.remote.api.ApiService
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response



@Composable
fun AttendanceSummaryScreen(
    studentId: String,
    department: String,
    apiService: ApiService

) {
    var attendanceData by remember { mutableStateOf<Map<String, Triple<Int, Int, Int>>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(true) {
        apiService.getAttendanceSummary(studentId, department).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        try {
                            val json = JSONObject(responseBody.string())
                            val summary = json.getJSONObject("summary")

                            val result = mutableMapOf<String, Triple<Int, Int, Int>>()

                            summary.keys().forEach { subject ->
                                val record = summary.getJSONObject(subject)
                                val held = record.getInt("held")
                                val attended = record.getInt("attended")
                                val percent = record.getInt("percentage")
                                result[subject] = Triple(held, attended, percent)
                            }

                            attendanceData = result
                        } catch (e: Exception) {
                            errorMessage = "Parsing error: ${e.message}"
                        }
                    }
                } else {
                    errorMessage = "Server error: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                errorMessage = "Network error: ${t.message}"
            }
        })
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Attendance Summary", fontWeight = FontWeight.Bold, fontSize = 20.sp)

        when {
            attendanceData != null -> {
                LazyColumn {
                    attendanceData!!.forEach { (subject, data) ->
                        val (held, attended, percentage) = data
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (percentage >= 75) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Subject: $subject", fontWeight = FontWeight.Bold)
                                    Text("Held: $held")
                                    Text("Attended: $attended")
                                    Text("Percentage: $percentage%", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
            errorMessage != null -> {
                Text("⚠️ $errorMessage", color = MaterialTheme.colorScheme.error)
            }
            else -> {
                CircularProgressIndicator()
            }
        }
    }
}
