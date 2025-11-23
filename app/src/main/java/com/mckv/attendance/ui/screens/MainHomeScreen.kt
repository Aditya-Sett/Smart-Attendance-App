package com.mckv.attendance.ui.screens

import com.mckv.attendance.utils.CheckBleSupport
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun MainHomeScreen(navController: NavHostController) {
    CheckBleSupport(LocalContext.current)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        LoginBox("Student Login", color = Color(0xFF1E2A78)) {
            navController.navigate("login_screen/ROLE_STUDENT") // Continue existing flow,, ✅ Passing role
        }

        Spacer(modifier = Modifier.height(16.dp))

        LoginBox("Administrator Login", color = Color(0xFFEF6C00)) {
            // Future: Navigate to admin screen
             navController.navigate("login_screen/ADMIN")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LoginBox("Teacher / Evaluator Login", color = Color(0xFF2E7D32)) {
            // Future: Navigate to teacher screen
            //navController.navigate("teacher") // 👈 Navigates to the new TeacherScreen
            navController.navigate("login_screen/ROLE_TEACHER")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LoginBox("College / Center Login", color = Color(0xFF6A1B9A)) {
            // Future: Navigate to center screen
            // navController.navigate("login_screen/ROLE_CENTER")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LoginBox("Results", color = Color(0xFFC62828)) {
            // Future: Navigate to results screen
        }
    }
}

@Composable
fun LoginBox(title: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
