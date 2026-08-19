// com/mckv/attendance/ui/screens/ChangePasswordScreen.kt
package com.mckv.attendance.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mckv.attendance.data.local.SessionManager
import com.mckv.attendance.data.remote.RetrofitClient
import com.mckv.attendance.data.remote.dto.request.ChangePasswordRequest
import com.mckv.attendance.data.remote.dto.response.ApiResponse
import com.mckv.attendance.utils.logoutUser
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(navController: NavController) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isCurrentPasswordVisible by remember { mutableStateOf(false) }
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val blueGradient = Brush.horizontalGradient(
        listOf(Color(0xFF81D4FA), Color(0xFF2196F3))
    )
    val buttonGradient = Brush.horizontalGradient(
        listOf(Color(0xFF4FC3F7), Color(0xFF1E88E5))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Password", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Update Your Password",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1E88E5)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 1. CURRENT PASSWORD
                PasswordFieldItem(
                    value = currentPassword,
                    onValueChange = { currentPassword = it.trim() },
                    placeholder = "Current Password",
                    isVisible = isCurrentPasswordVisible,
                    onToggleVisibility = { isCurrentPasswordVisible = !isCurrentPasswordVisible },
                    gradient = blueGradient
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 2. NEW PASSWORD
                PasswordFieldItem(
                    value = newPassword,
                    onValueChange = { newPassword = it.trim() },
                    placeholder = "New Password",
                    isVisible = isNewPasswordVisible,
                    onToggleVisibility = { isNewPasswordVisible = !isNewPasswordVisible },
                    gradient = blueGradient
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. CONFIRM NEW PASSWORD
                PasswordFieldItem(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it.trim() },
                    placeholder = "Confirm New Password",
                    isVisible = isConfirmPasswordVisible,
                    onToggleVisibility = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                    gradient = blueGradient
                )

                Spacer(modifier = Modifier.height(28.dp))

                // SET PASSWORD BUTTON
                Button(
                    onClick = {
                        focusManager.clearFocus()

                        if (currentPassword.isBlank()) {
                            Toast.makeText(context, "Please enter your current password", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPassword.isBlank()) {
                            Toast.makeText(context, "Please enter a new password", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            Toast.makeText(context, "New passwords do not match!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val token = SessionManager.authToken
                        if (token.isNullOrEmpty()) {
                            Toast.makeText(context, "Session expired, please log in again", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isLoading = true
                        val request = ChangePasswordRequest(
                            password = currentPassword,
                            newPassword = newPassword
                        )

                        RetrofitClient.authInstance.changePassword("Bearer $token", request)
                            .enqueue(object : Callback<ApiResponse<Map<String, Boolean>?>> {
                                override fun onResponse(
                                    call: Call<ApiResponse<Map<String, Boolean>?>>,
                                    response: Response<ApiResponse<Map<String, Boolean>?>>
                                ) {
                                    isLoading = false
                                    val body = response.body()
                                    if (response.isSuccessful && body?.success == true) {
                                        Toast.makeText(
                                            context,
                                            body.message ?: "Password changed successfully!",
                                            Toast.LENGTH_LONG
                                        ).show()
//                                        navController.popBackStack()
                                        logoutUser(context, navController)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            body?.message ?: "Failed to change password (${response.code()})",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }

                                override fun onFailure(
                                    call: Call<ApiResponse<Map<String, Boolean>?>>,
                                    t: Throwable
                                ) {
                                    isLoading = false
                                    Toast.makeText(context, "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
                                }
                            })
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
                                text = "Set Password",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordFieldItem(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    gradient: Brush
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(gradient),
        contentAlignment = Alignment.CenterStart
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.White.copy(0.8f)) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White) },
            trailingIcon = {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            },
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
}