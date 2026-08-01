
package com.mckv.attendance.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mckv.attendance.data.remote.RetrofitClient
import com.mckv.attendance.data.remote.dto.request.BaseResponse
import com.mckv.attendance.data.remote.dto.request.DeviceStatusRequest
import com.mckv.attendance.data.remote.dto.request.LoginRequest
import com.mckv.attendance.utils.DeviceUtils
import com.mckv.attendance.utils.UserPreferences
import com.mckv.attendance.utils.loginUser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    var usernameError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }

    // Dialog state management
    var showDeviceDialog by remember { mutableStateOf(false) }
    var isCheckingStatus by remember { mutableStateOf(false) }
    var isSendingRequest by remember { mutableStateOf(false) }
    var deviceStatusMessage by remember { mutableStateOf<String?>(null) }
    var isStatusSuccess by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // 🔄 Auto-fill saved credentials on screen launch
    LaunchedEffect(Unit) {
        val (isRemembered, savedUsername, savedPassword) = UserPreferences.getSavedCredentials(context).first()
        if (isRemembered) {
            rememberMe = true
            username = savedUsername
            password = savedPassword
        }
    }

    // 🌈 BLUE THEME
    val blueGradient = Brush.horizontalGradient(
        listOf(Color(0xFF81D4FA), Color(0xFF2196F3))
    )

    val buttonGradient = Brush.horizontalGradient(
        listOf(Color(0xFF4FC3F7), Color(0xFF1E88E5))
    )

    // 📱 Keyboard detection
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

    val animatedOffset by animateDpAsState(
        targetValue = if (isKeyboardOpen) (-40).dp else 0.dp,
        label = "keyboardAnim"
    )

    // Check Device Request Status function
    fun checkDeviceStatus() {
        showDeviceDialog = true
        isCheckingStatus = true
        deviceStatusMessage = null

        val req = DeviceStatusRequest(email = username, password = password)
        RetrofitClient.authInstance.checkDeviceRequestStatus(req)
            .enqueue(object : Callback<BaseResponse<String>> {
                override fun onResponse(
                    call: Call<BaseResponse<String>>,
                    response: Response<BaseResponse<String>>
                ) {
                    isCheckingStatus = false
                    val res = response.body()

                    // If backend returns success == true for the status check
                    if (response.isSuccessful && res != null && res.success) {
                        if (res.data == "REBIND") {
                            // Success True but data is REBIND
                            isStatusSuccess = false
                            deviceStatusMessage = "Do you want to open your account here?"
                        }
                        else {
                            isStatusSuccess = true
                            // Show existing request status (e.g., "APPROVED", "PENDING", etc.)
                            deviceStatusMessage = "Device Request Status: ${res.data ?: res.message} Contact to your Class Coordinator"
                        }
                    } else {
                        // No active request found -> Prompt to initiate a change request
                        isStatusSuccess = false
                        deviceStatusMessage = "Do you want to open your account here?"
                    }
                }

                override fun onFailure(call: Call<BaseResponse<String>>, t: Throwable) {
                    isCheckingStatus = false
                    isStatusSuccess = false
                    deviceStatusMessage = "Do you want to open your account here?"
                    Log.e("DeviceStatusError", "Error: ${t.localizedMessage}", t)
                }
            })
    }

    // Request Device Change function
    fun sendDeviceChangeRequest() {
        isSendingRequest = true
        val req = DeviceStatusRequest(email = username, password = password)
        RetrofitClient.authInstance.requestDeviceChange(req)
            .enqueue(object : Callback<BaseResponse<Nothing>> {
                override fun onResponse(
                    call: Call<BaseResponse<Nothing>>,
                    response: Response<BaseResponse<Nothing>>
                ) {
                    isSendingRequest = false
                    val res = response.body()
                    if (response.isSuccessful && res != null && res.success) {
                        // Change request submitted -> Switch dialog view to success confirmation
                        isStatusSuccess = true
                        deviceStatusMessage = res.message ?: "DEVICE_CHANGE_REQUESTED"
                    } else {
                        deviceStatusMessage = "Failed to send request. Please try again."
                    }
                }

                override fun onFailure(call: Call<BaseResponse<Nothing>>, t: Throwable) {
                    isSendingRequest = false
                    deviceStatusMessage = "Network error while sending request."
                }
            })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6FA))
            .imePadding()
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                }
            }
    ) {

        // 🔵 LEFT ARC
        Box(
            modifier = Modifier
                .size(420.dp)
                .offset(x = (-220).dp, y = (-300).dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF81D4FA), Color(0xFF2196F3))
                    )
                )
        )

        // 🔵 RIGHT ARC
        Box(
            modifier = Modifier
                .size(420.dp)
                .align(Alignment.TopEnd)
                .offset(x = 220.dp, y = (-300).dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF4FC3F7), Color(0xFF1E88E5))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .offset(y = animatedOffset)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Spacer(modifier = Modifier.height(110.dp))

            // 👤 PROFILE ICON
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(blueGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // USERNAME
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(blueGradient),
                contentAlignment = Alignment.CenterStart
            ) {
                TextField(
                    value = username,
                    onValueChange = {
                        username = it.trim()
                        usernameError = ""
                    },
                    placeholder = {
                        Text("Email", color = Color.White.copy(0.8f))
                    },
                    singleLine = true,
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
                        .padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // PASSWORD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(blueGradient),
                contentAlignment = Alignment.CenterStart
            ) {
                TextField(
                    value = password,
                    onValueChange = {
                        password = it.trim()
                        passwordError = ""
                    },
                    placeholder = {
                        Text("Password", color = Color.White.copy(0.8f))
                    },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            passwordVisible = !passwordVisible
                        }) {
                            Icon(
                                if (passwordVisible)
                                    Icons.Default.Visibility
                                else
                                    Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None
                    else PasswordVisualTransformation(),
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
                        .padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // REMEMBER ME & FORGOT PASSWORD ROW
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF2196F3)
                        )
                    )
                    Text("Remember me", style = MaterialTheme.typography.bodyMedium)
                }

                Text(
                    text = "Forgot password?",
                    color = Color(0xFF1E88E5),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.clickable {
                        navController.navigate("forgot_password")
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SIGN IN BUTTON
            Button(
                onClick = {

                    var hasError = false

                    if (username.isBlank()) {
                        usernameError = "Plz enter your Email"
                        hasError = true
                    }

                    if (password.isBlank()) {
                        passwordError = "Plz enter your password"
                        hasError = true
                    }

                    if (hasError) return@Button

                    keyboardController?.hide()
                    focusManager.clearFocus()

                    // 💾 Save or clear credentials asynchronously
                    coroutineScope.launch {
                        UserPreferences.saveCredentials(context, rememberMe, username, password)
                    }

                    val deviceId = DeviceUtils.getDeviceId(context)

                    val request = LoginRequest(username, password, deviceHardwareId = deviceId)
                    loading = true

                    loginUser(
                        request = request,
                        context = context,
                        navController = navController,
                        onUnrecognizedDevice = {
                            checkDeviceStatus()
                        },
                        onComplete = {
                            loading = false
                        }
                    )

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
                    if (loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Sign in", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have any account? ",
                    color = Color.Gray
                )

                Text(
                    text = "Sign up",
                    color = Color(0xFF1E88E5),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.clickable {
                        navController.navigate("signup")
                    }
                )
            }
        }

        // UNRECOGNIZED DEVICE POPUP DIALOG
        if (showDeviceDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!isCheckingStatus && !isSendingRequest) {
                        showDeviceDialog = false
                    }
                },
                title = {
                    Text(
                        text = if (isStatusSuccess) "Request Status" else "Device Verification",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCheckingStatus || isSendingRequest) {
                            CircularProgressIndicator()
                        } else {
                            Text(
                                text = deviceStatusMessage ?: "",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                },
                confirmButton = {
                    if (!isCheckingStatus && !isSendingRequest) {
                        if (isStatusSuccess) {
                            // Status is already retrieved OR request was successfully submitted -> Dismiss
                            TextButton(
                                onClick = { showDeviceDialog = false }
                            ) {
                                Text("OK")
                            }
                        } else {
                            // Status check returned false -> Action to submit change request
                            TextButton(
                                onClick = { sendDeviceChangeRequest() }
                            ) {
                                Text("Yes")
                            }
                        }
                    }
                },
                dismissButton = {
                    // Show "No" button only when prompting to open account on this device
                    if (!isCheckingStatus && !isSendingRequest && !isStatusSuccess) {
                        TextButton(
                            onClick = { showDeviceDialog = false }
                        ) {
                            Text("No")
                        }
                    }
                }
            )
        }

        // LOADING OVERLAY
        AnimatedVisibility(
            visible = loading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}