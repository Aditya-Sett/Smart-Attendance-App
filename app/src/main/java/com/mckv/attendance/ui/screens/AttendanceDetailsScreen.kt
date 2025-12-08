package com.mckv.attendance.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mckv.attendance.data.remote.RetrofitClient
import com.mckv.attendance.ui.components.common.CommonTopBar
import com.mckv.attendance.utils.interactionDetection
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@Composable
fun AttendanceDetailsScreen(
    teacherId: String,
    code: String,
    generatedAt: String,
    expiresAt: String,
    navController: NavController
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var students by remember { mutableStateOf<List<StudentStatus>>(emptyList()) }

    LaunchedEffect(Unit) {

        val json = JSONObject().apply {
            put("teacherId", teacherId)
            put("code", code)
            put("generatedAt", generatedAt)
            put("expiresAt", expiresAt)
        }

        try {
            val response = RetrofitClient.instance.getAttendanceDetails(
                json.toString().toRequestBody("application/json".toMediaType())
            )
            val body = response.string()
            val root = JSONObject(body)

            if (root.getBoolean("success")) {
                val arr = root.getJSONArray("records")

                val list = mutableListOf<StudentStatus>()

                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        StudentStatus(
                            roll = obj.getString("roll"),
                            name = obj.getString("name"),
                            status = obj.getString("status")
                        )
                    )
                }
                students = list
            } else {
                error = root.getString("message")
            }

        } catch (e: Exception) {
            error = e.message
        }

        loading = false
    }

    when {
        loading -> Text("Loading...")
        error != null -> Text("Error: $error")
        else -> StudentStatusTable(navController,students)
    }
}

@Composable
fun StudentStatusTable(navController: NavController,students: List<StudentStatus>) {
    Scaffold(
        modifier = Modifier.interactionDetection(),
        topBar = {
            Column {
                CommonTopBar(
                    title = "Attendance Records",
                    navController = navController
                )
            }
        }
    ) {
            innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Roll", fontWeight = FontWeight.Bold)
                        Text("Name", fontWeight = FontWeight.Bold)
                        Text("Status", fontWeight = FontWeight.Bold)
                    }
                    Divider()
                }

                items(students) { student ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(student.roll)
                        Text(student.name)
                        Text(
                            student.status,
                            color = if (student.status == "P") Color(0xFF008000) else Color.Red
                        )
                    }
                    Divider()
                }
            }
        }
    }
}

data class StudentStatus(
    val roll: String,
    val name: String,
    val status: String
)


