package com.mckv.attendance.ui.screens


import android.provider.CalendarContract.Colors
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mckv.attendance.R
import com.mckv.attendance.data.remote.dto.request.LoginRequest
import com.mckv.attendance.utils.loginUser
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.platform.LocalDensity


@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, roleFromNav: String) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var usernameError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val offsetY by animateDpAsState(
        targetValue = if (keyboardOpen) (-40).dp else 0.dp, // adjust lift
        animationSpec = tween(durationMillis = 20)
    )

    // Background gradient
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFC541D1),
            Color.White
        ),
        start = Offset(0f, 0f),      // top-left
        end = Offset.Infinite        // bottom-right
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())   // 👈 allows scroll when keyboard appears
                .animateContentSize()
                .padding(24.dp),

            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Logo and welcome section


            Spacer(modifier = Modifier.height(4.dp))

            if(!keyboardOpen) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Login",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Medium,
                    fontSize = 40.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
            }


            Spacer(modifier = Modifier.height(3.dp))

            // Error message
            AnimatedVisibility(
                visible = errorMessage.isNotEmpty(),
                enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = fadeOut(animationSpec = tween(durationMillis = 300))
            ) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Login form
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = offsetY)  // 👈 smooth lift
                    .padding(top = 40.dp),
//                    .imePadding(), // push card down so avatar can "overlap"
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp), // top padding so content starts below avatar
                    horizontalAlignment = Alignment.CenterHorizontally,

                    verticalArrangement = Arrangement.Center
                ) {


                    // Circle Avatar with Person Icon
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .background(
                                color = Color(0xFFC232D9),
                                shape = CircleShape
                            )
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Icon",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(120.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Username field - FIXED: maxLines = 1 instead of singleLine
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            usernameError = "" // Clear error when user starts typing
                        },
                        label = { Text("Username")},
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        maxLines = 1, // Fixed: Replaced singleLine with maxLines

                        isError = usernameError.isNotEmpty(),

                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF9D34ED),
                            unfocusedBorderColor = Color.Gray,
                            errorBorderColor = Color.Red,
                            focusedLabelColor = Color(0xFF9D34ED),
                            unfocusedLabelColor = Color.Gray,
                            errorLabelColor = Color.Red,
                            cursorColor = Color(0xFF9D34ED)
                        )

                    )

                    // Username error text
                    if (usernameError.isNotEmpty()) {
                        Text(
                            text = usernameError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password field - FIXED: maxLines = 1 instead of singleLine
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = "" // Clear error when user starts typing
                        },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) {
                                        Icons.Default.Visibility
                                    } else {
                                        Icons.Default.VisibilityOff
                                    },
                                    contentDescription = if (passwordVisible) {
                                        "Hide password"
                                    } else {
                                        "Show password"
                                    }
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        ),
                        maxLines = 1, // Fixed: Replaced singleLine with maxLines
                        isError = passwordError.isNotEmpty(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF9D34ED),
                            unfocusedBorderColor = Color.Gray,
                            errorBorderColor = Color.Red,
                            focusedLabelColor = Color(0xFF9D34ED),
                            unfocusedLabelColor = Color.Gray,
                            errorLabelColor = Color.Red,
                            cursorColor = Color(0xFF9D34ED)
                        )
                    )

                    // Password error text
                    if (passwordError.isNotEmpty()) {
                        Text(
                            text = passwordError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Forgot password
                    Text(
                        text = "Forgot Password?",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable {
                                // Navigate to forgot password screen
                            }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login button
                    ElevatedButton(
                        onClick = {

                            //HAS ERROR
                            var hasError=false

                            // Validate username
                            if (username.isBlank()) {
                                usernameError = "Plz enter your username"
                                hasError = true
                            } else {
                                usernameError = ""
                            }

                            // Validate password
                            if (password.isBlank()) {
                                passwordError = "Plz enter your password"
                                hasError = true
                            } else {
                                passwordError = ""
                            }

                            // If any field has error, stop here
                            if (hasError) {
                                return@ElevatedButton
                            }

                            keyboardController?.hide()
                            focusManager.clearFocus()

                            val request = LoginRequest(username, password, role = roleFromNav)
                            loading = true
                            errorMessage = ""

//                          Fixed callback syntax
                            loginUser(request, context, navController) {
                                loading = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !loading,
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.elevatedButtonElevation( // 👈 shadow control
                            defaultElevation = 6.dp,
                            pressedElevation = 10.dp,
                            focusedElevation = 8.dp,
                            hoveredElevation = 8.dp
                        ),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color(0xFFC232D9),
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text =stringResource( R.string.login),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))



            Spacer(modifier = Modifier.weight(0.3f))
        }

        // Loading overlay
        AnimatedVisibility(
            visible = loading,
            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = fadeOut(animationSpec = tween(durationMillis = 300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
            }
        }
    }
}





//@Composable
//fun LoginScreen(navController: NavController, roleFromNav: String) {
//    var username by remember { mutableStateOf("") }
//    var password by remember { mutableStateOf("") }
//    var loading by remember { mutableStateOf(false) }
//    val context = LocalContext.current
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .statusBarsPadding()
//            .navigationBarsPadding()
//            .padding(16.dp)
//    ) {
//        Text("Login", style = MaterialTheme.typography.headlineMedium)
//        Spacer(modifier = Modifier.height(16.dp))
//
//        OutlinedTextField(
//            value = username,
//            onValueChange = { username = it },
//            label = { Text("Username") },
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        OutlinedTextField(
//            value = password,
//            onValueChange = { password = it },
//            label = { Text("Password") },
//            modifier = Modifier.fillMaxWidth(),
//            visualTransformation = PasswordVisualTransformation()
//        )
//
//        Spacer(modifier = Modifier.height(24.dp))
//
//        Button(
//            onClick = {
//                val request = LoginRequest(username, password, role = roleFromNav)
//                loading = true
//                loginUser(request, context, navController){
//                    loading = false
//                }
//            },
//            modifier = Modifier.fillMaxWidth(),
//            enabled = !loading
//        ) {
//            Text("Login")
//        }
//    }
//    if (loading) {  // ✅ overlay spinner
//        Box(
//            modifier = Modifier.fillMaxSize(),
//            contentAlignment = Alignment.Center
//        ) {
//            CircularProgressIndicator()
//        }
//    }
//}
