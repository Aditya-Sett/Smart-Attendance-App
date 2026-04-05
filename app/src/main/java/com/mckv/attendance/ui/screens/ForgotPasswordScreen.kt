package com.mckv.attendance.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.material3.OutlinedTextField
import com.mckv.attendance.data.remote.RetrofitClient
import android.widget.Toast

import okhttp3.ResponseBody

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(navController: NavController) {

    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }

    var emailError by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf("") }

    var step by remember { mutableStateOf(1) } // 1 = email, 2 = otp
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    val context = LocalContext.current

    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFFC541D1), Color.White),
        start = Offset(0f, 0f),
        end = Offset.Infinite
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = "Forgot Password",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {

                    // 🔹 MESSAGE
                    if (message.isNotEmpty()) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    // =========================
                    // STEP 1: EMAIL
                    // =========================
                    if (step == 1) {

                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                emailError = ""
                            },
                            label = { Text("Enter Email") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = emailError.isNotEmpty()
                        )

                        if (emailError.isNotEmpty()) {
                            Text(emailError, color = Color.Red)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {

                                if (email.isBlank()) {
                                    emailError = "Email required"
                                    return@Button
                                }

                                loading = true

                                // 🔥 CALL API
                                forgotPasswordEmail(email, context) {
                                    loading = false
                                    step = 2
                                    message = "OTP sent to your email"
                                }

                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !loading
                        ) {
                            Text("Send OTP")
                        }
                    }

                    // =========================
                    // STEP 2: OTP VERIFY
                    // =========================
                    if (step == 2) {

                        OutlinedTextField(
                            value = otp,
                            onValueChange = {
                                otp = it
                                otpError = ""
                            },
                            label = { Text("Enter OTP") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            isError = otpError.isNotEmpty()
                        )

                        if (otpError.isNotEmpty()) {
                            Text(otpError, color = Color.Red)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {

                                if (otp.isBlank()) {
                                    otpError = "OTP required"
                                    return@Button
                                }

                                loading = true

                                verifyOtp(email, otp, context) {
                                    loading = false
                                    message = "OTP Verified ✅"

                                    // 👉 Navigate to Reset Password screen later
                                    // navController.navigate("reset_password")
                                }

                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !loading
                        ) {
                            Text("Verify OTP")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Resend OTP",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                forgotPasswordEmail(email, context) {}
                            }
                        )
                    }
                }
            }
        }

        // 🔄 LOADING OVERLAY
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

fun forgotPasswordEmail(
    email: String,
    context: Context,
    onSuccess: () -> Unit
) {
    RetrofitClient.authInstance.forgotPassword(email).enqueue(object : Callback<ResponseBody> {
        override fun onResponse(
            call: Call<ResponseBody>,
            response: Response<ResponseBody>
        ) {
            if (response.isSuccessful) {
                Toast.makeText(context, "OTP sent successfully", Toast.LENGTH_SHORT).show()
                onSuccess()
            } else {
                Toast.makeText(context, "Failed to send OTP", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
        }
    })
}

fun verifyOtp(
    email: String,
    otp: String,
    context: Context,
    onSuccess: () -> Unit
) {
    val call = RetrofitClient.authInstance.verifyPasswordEmail(email, otp)

    call.enqueue(object : Callback<ResponseBody> {
        override fun onResponse(
            call: Call<ResponseBody>,
            response: Response<ResponseBody>
        ) {
            if (response.isSuccessful) {
                Toast.makeText(context, "OTP Verified", Toast.LENGTH_SHORT).show()
                onSuccess()
            } else {
                Toast.makeText(context, "Invalid OTP", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
        }
    })
}