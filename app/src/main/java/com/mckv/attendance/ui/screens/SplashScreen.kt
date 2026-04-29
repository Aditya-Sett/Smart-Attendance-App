/*package com.mckv.attendance.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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

            val roles = SessionManager.userDetails?.role ?: emptyList()

            val target = when {
                roles.contains("STUDENT") -> "home"

                roles.isNotEmpty() -> "dynamic_dashboard"

                else -> null
            }

            if (target != null) {
                navController.navigate(target) {
                    popUpTo("splash_screen") { inclusive = true }
                }
            } else {
                redirectToMainHome(navController, "Unknown role")
            }
        } else {
            // Handle expired or invalid token
            val token = SessionManager.authToken
            if (SessionManager.isLoggedIn && token != null) {
                if (SessionManager.isTokenExpired()) {
                    val expiryTime = JwtUtils.getTokenExpiryDate(token)
                    System.out.println("🔐 Token expired at: $expiryTime")
                    SessionManager.logout()
                    Toast.makeText(
                        context,
                        "Session expired, please login again",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    System.out.println("🔐 Invalid token format")
                    SessionManager.logout()
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
    navController.navigate("login_screen") {
        popUpTo("splash_screen") { inclusive = true }
    }
}*/
package com.mckv.attendance.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
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

    // ================= LOGIC =================
    LaunchedEffect(Unit) {
        delay(1500)

        val isLoggedIn = SessionManager.isLoggedIn &&
                !SessionManager.authToken.isNullOrEmpty() &&
                !SessionManager.isTokenExpired()

        if (isLoggedIn) {
            val token = SessionManager.authToken!!
            val roles = SessionManager.userDetails?.role ?: emptyList()

            val target = when {
                roles.contains("STUDENT") -> "home"
                roles.isNotEmpty() -> "dynamic_dashboard"
                else -> null
            }

            if (target != null) {
                navController.navigate(target) {
                    popUpTo("splash_screen") { inclusive = true }
                }
            } else {
                redirectToMainHome(navController, "Unknown role")
            }

        } else {
            val token = SessionManager.authToken
            if (SessionManager.isLoggedIn && token != null) {
                if (SessionManager.isTokenExpired()) {
                    SessionManager.logout()
                    Toast.makeText(
                        context,
                        "Session expired, please login again",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    SessionManager.logout()
                }
            }
            redirectToMainHome(navController, "Please login")
        }
    }

    // ================= UI =================

    // Floating animation
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    // Entry animations
    val scale = remember { Animatable(0.8f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(800, easing = EaseOutBack))
        alpha.animateTo(1f, animationSpec = tween(800))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D47A1),
                        Color(0xFF1976D2)
                    )
                )
            )
    ) {

        // 🔵 Top Right Circle
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .clip(CircleShape)
                .background(Color(0xFF29B6F6))
        )

        // ⚪ Bottom Left Circle
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-80).dp, y = 80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
        )

        // ✨ Animated Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = floatAnim.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "SMART",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
            )

            Text(
                text = "ATTENDANCE APP",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                visible = alpha.value > 0.7f,
                enter = fadeIn(tween(600))
            ) {
                Text(
                    text = "Attend today, Make tomorrow",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            AnimatedVisibility(
                visible = alpha.value > 0.9f,
                enter = fadeIn(tween(600))
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

private fun redirectToMainHome(navController: NavController, reason: String = "") {
    System.out.println("🔀 Redirecting to main home: $reason")
    navController.navigate("login_screen") {
        popUpTo("splash_screen") { inclusive = true }
    }
}

