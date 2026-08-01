// com/mckv/attendance/ui/screens/ForgotPasswordScreen.kt
package com.mckv.attendance.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mckv.attendance.data.remote.RetrofitClient
import com.mckv.attendance.data.remote.dto.request.VerifyOtpRequest
import com.mckv.attendance.data.remote.dto.response.ApiResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun ForgotPasswordScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }

    var isOtpSent by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val blueGradient = Brush.horizontalGradient(
        listOf(Color(0xFF81D4FA), Color(0xFF2196F3))
    )
    val buttonGradient = Brush.horizontalGradient(
        listOf(Color(0xFF4FC3F7), Color(0xFF1E88E5))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6FA))
            .imePadding()
            .pointerInput(Unit) {
                detectTapGestures { focusManager.clearFocus() }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Forgot Password",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1E88E5)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (!isOtpSent) "Enter your email to receive an OTP code" else "Enter the OTP sent to $email",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(30.dp))

            // EMAIL INPUT
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(blueGradient),
                contentAlignment = Alignment.CenterStart
            ) {
                TextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    placeholder = { Text("Email", color = Color.White.copy(0.8f)) },
                    singleLine = true,
                    enabled = !isOtpSent, // Disable editing once OTP is sent
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.White) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        disabledTextColor = Color.White.copy(0.7f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // OTP INPUT FIELD (Visible only after OTP is sent)
            AnimatedVisibility(visible = isOtpSent) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(blueGradient),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        TextField(
                            value = otp,
                            onValueChange = { otp = it.trim() },
                            placeholder = { Text("Enter OTP", color = Color.White.copy(0.8f)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Color.White,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // ACTION BUTTON (Send OTP or Verify)
            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (!isOtpSent) {
                        // Validate Email & Send OTP (GET Request)
                        if (email.isBlank()) {
                            Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isLoading = true
                        RetrofitClient.authInstance.sendForgotPasswordOtp(email)
                            .enqueue(object : Callback<ApiResponse<String?>> {
                                override fun onResponse(
                                    call: Call<ApiResponse<String?>>,
                                    response: Response<ApiResponse<String?>>
                                ) {
                                    isLoading = false
                                    val body = response.body()
                                    if (response.isSuccessful && body?.success == true) {
                                        isOtpSent = true
                                        Toast.makeText(context, body.message ?: "OTP sent successfully!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, body?.message ?: "Failed to send OTP", Toast.LENGTH_LONG).show()
                                    }
                                }

                                override fun onFailure(call: Call<ApiResponse<String?>>, t: Throwable) {
                                    isLoading = false
                                    Toast.makeText(context, "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
                                }
                            })
                    } else {
                        // Validate OTP & Submit Verification (POST Request)
                        if (otp.isBlank()) {
                            Toast.makeText(context, "Please enter the OTP", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isLoading = true
                        val request = VerifyOtpRequest(email = email, otp = otp)
                        RetrofitClient.authInstance.verifyPasswordOtp(request)
                            .enqueue(object : Callback<ApiResponse<String?>> {
                                override fun onResponse(
                                    call: Call<ApiResponse<String?>>,
                                    response: Response<ApiResponse<String?>>
                                ) {
                                    isLoading = false
                                    val body = response.body()
                                    if (response.isSuccessful && body?.success == true) {
                                        Toast.makeText(context, body.message ?: "OTP verified successfully!", Toast.LENGTH_LONG).show()
                                        // Navigate to reset password page or back to login screen
                                        navController.popBackStack()
                                    } else {
                                        Toast.makeText(context, body?.message ?: "Invalid or Expired OTP", Toast.LENGTH_LONG).show()
                                    }
                                }

                                override fun onFailure(call: Call<ApiResponse<String?>>, t: Throwable) {
                                    isLoading = false
                                    Toast.makeText(context, "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
                                }
                            })
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(buttonGradient),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (!isOtpSent) "Send OTP" else "Verify",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Top Bar Back Button
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .padding(top = 16.dp, start = 16.dp)
                .statusBarsPadding()
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF2196F3))
        }
    }
}