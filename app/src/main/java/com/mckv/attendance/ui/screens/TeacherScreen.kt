package com.mckv.attendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mckv.attendance.data.local.SessionManager
import com.mckv.attendance.ui.components.common.CommonTopBar
import com.mckv.attendance.utils.UserInteractionHandler
import com.mckv.attendance.utils.interactionDetection

@Composable
fun TeacherScreen(navController: NavHostController) {

    val teacherId = SessionManager.teacherId ?: "Unknown"
    val department = SessionManager.department ?: "Not Specified"

    // 🔹 Add User Interaction Handler for Token Expiry Detection
    UserInteractionHandler(navController = navController)

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
                    Text("Welcome, $teacherId", color = Color.White)
                    Text("Department: $department", color = Color.White)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    /* TODO: Navigate to Add Schedule screen */
                    navController.navigate("add_schedule")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Add New Schedule")
            }

            Button(
                onClick = { /* TODO: Navigate to Edit Schedule screen */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Edit Schedule")
            }

            Button(
                onClick = { /* TODO: Navigate to Delete Schedule screen */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Delete Schedule")
            }

            Button(
                onClick = { navController.navigate("take_attendance") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Take Attendance")
            }

            Button(
                onClick = { navController.navigate("students_attendance_summary") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Students Attendance History")
            }

            Button(
                onClick = { navController.navigate("export_attendance") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Show Attendance Records")
            }

            Button(
                onClick = { navController.navigate("consider_absence") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Consider Absence")
            }
        }
    }
}
