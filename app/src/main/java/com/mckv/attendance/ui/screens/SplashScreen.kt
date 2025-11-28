package com.mckv.attendance.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mckv.attendance.data.local.SessionManager
import com.mckv.attendance.utils.JwtUtils
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current

    LaunchedEffect(key1 = Unit) {
        // Debug current session status
        SessionManager.printSessionStatus()

        // Simulate some loading time
        delay(1500)

        // Check if user is already logged in AND token is not expired
        val isLoggedIn = SessionManager.isLoggedIn &&
                !SessionManager.authToken.isNullOrEmpty() &&
                !SessionManager.isTokenExpired()

        if (isLoggedIn) {
            // Token is valid, navigate to appropriate screen
            val token = SessionManager.authToken!!
            val timeUntilExpiry = JwtUtils.getTimeUntilExpiry(token)

            System.out.println("✅ Token valid, expires in: $timeUntilExpiry")

            when (SessionManager.userRole) {
                "ROLE_STUDENT" -> {
                    System.out.println("🔄 Auto-redirecting to Student Dashboard")
                    navController.navigate("home") {
                        popUpTo("splash_screen") { inclusive = true }
                    }
                }
                "ROLE_TEACHER" -> {
                    System.out.println("🔄 Auto-redirecting to Teacher Dashboard")
                    navController.navigate("teacher") {
                        popUpTo("splash_screen") { inclusive = true }
                    }
                }
                "ADMIN" -> {
                    System.out.println("🔄 Auto-redirecting to Admin Dashboard")
                    navController.navigate("admin_dashboard") {
                        popUpTo("splash_screen") { inclusive = true }
                    }
                }
                else -> {
                    redirectToMainHome(navController, "Unknown role")
                }
            }
        } else {
            // Handle expired or invalid token
            val token = SessionManager.authToken
            if (SessionManager.isLoggedIn && token != null) {
                if (SessionManager.isTokenExpired()) {
                    val expiryTime = JwtUtils.getTokenExpiryDate(token)
                    System.out.println("🔐 Token expired at: $expiryTime")
                    SessionManager.clear()
                    Toast.makeText(context, "Session expired, please login again", Toast.LENGTH_LONG).show()
                } else {
                    System.out.println("🔐 Invalid token format")
                    SessionManager.clear()
                }
            } else {
                System.out.println("🔐 No active session")
            }
            redirectToMainHome(navController, "Please login")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFC541D1)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Attendance App",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Checking login status...",
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

private fun redirectToMainHome(navController: NavController, reason: String = "") {
    System.out.println("🔀 Redirecting to main home: $reason")
    navController.navigate("main_home") {
        popUpTo("splash_screen") { inclusive = true }
    }
}