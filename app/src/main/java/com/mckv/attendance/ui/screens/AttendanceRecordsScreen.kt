package com.mckv.attendance.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
//import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.mckv.attendance.data.local.SessionManager
import com.mckv.attendance.data.local.SessionManager.teacherId
import com.mckv.attendance.data.remote.RetrofitClient
import com.mckv.attendance.ui.components.common.CommonTopBar
import com.mckv.attendance.utils.interactionDetection
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Date

@Composable
fun AttendanceRecordsScreen(navcontroller : NavController) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var records by remember { mutableStateOf<List<AttendanceRecordItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        val teacherId = SessionManager.teacherId

        val json = JSONObject().apply {
            put("teacherId", teacherId)
        }

        try {
            val response = RetrofitClient.instance.attendanceTakenBySelf(
                json.toString().toRequestBody("application/json".toMediaType())
            )

            val body = response.string()  // because you use ResponseBody
            println("RAW JSON = $body")

            val root = JSONObject(body)
            if (root.getBoolean("success")) {
                val arr = root.getJSONArray("data")

                val temp = mutableListOf<AttendanceRecordItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    temp.add(
                        AttendanceRecordItem(
                            id = obj.getString("id"),
                            code = obj.getString("code"),
                            department = obj.getString("department"),
                            subject = obj.getString("subject"),
                            className = obj.getString("className"),
                            academicYear = obj.getString("academicYear"),
                            admissionYear = obj.getString("admissionYear"),
                            generatedAt = obj.getString("generatedAt"),
                            expiresAt = obj.getString("expiresAt"),
                            //wifiFingerprintCount = obj.getInt("wifiFingerprintCount"),
                            bluetoothUuid = obj.getString("bluetoothUuid")
                        )
                    )
                }

                records = temp
            } else {
                error = root.getString("message")
            }

        } catch (e: Exception) {
            error = e.message
        }

        loading = false
    }

    // UI states
    when {
        loading -> {
            Text(
                "Loading...",
                modifier = Modifier.fillMaxSize(),
                textAlign = TextAlign.Center
            )
        }

        error != null -> {
            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Error: $error")
                println("❌❌❌ $error")
                Button(onClick = { navcontroller.popBackStack() }) {
                    Text("Go Back")
                }
            }
        }

        else -> {
            teacherId?.let { AttendanceRecordList(it,navcontroller,records) }
        }
    }
}

//@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AttendanceRecordList(teacherId: String,navcontroller: NavController,records: List<AttendanceRecordItem>) {
    Scaffold(
        modifier = Modifier.interactionDetection(),
        topBar = {
            Column {
                CommonTopBar(
                    title = "Attendance Records",
                    navController = navcontroller
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(records) { record ->
                    AttendanceRecordCard(record) {
                        navcontroller.navigate(
                            "attendance_details/${teacherId}/${record.code}/${record.generatedAt}/${record.expiresAt}"
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun AttendanceRecordCard(record: AttendanceRecordItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                onClick()
            },
        elevation = CardDefaults.cardElevation(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent // Make card transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFEFDC90), // Whitish yellow
                            //Color(0xFFF4F1E8), // Lighter yellow tint
                            //Color(0xFFF8EED1), // Warm yellow
                            Color(0xFFF1DD5D)  // Chromish yellow
                        ),
                        start = Offset.Infinite, // Top right
                        end = Offset(0f, Float.POSITIVE_INFINITY) // Bottom left

                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text("Subject: ${record.subject}", fontWeight = FontWeight.Bold)
                Text("Code: ${record.code}")
                Text("Department: ${record.department}")
                Text("Class: ${record.className}")
                Text("Generated: ${record.generatedAt}")
            }
        }
    }
}

data class AttendanceRecordItem(
    val id: String,
    val code: String,
    val department: String,
    val subject: String,
    val className: String,
    val academicYear: String,
    val admissionYear: String,
    val generatedAt: String,
    val expiresAt: String,
    //val wifiFingerprintCount: Int,
    val bluetoothUuid: String
)
