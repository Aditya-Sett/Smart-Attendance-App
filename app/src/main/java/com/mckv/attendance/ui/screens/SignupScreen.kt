/*package com.mckv.attendance.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mckv.attendance.data.remote.RetrofitClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(navController: NavController) {
    val primaryColor = Color(0xFF1E88E5)
    val secondaryColor = Color(0xFF64B5F6)
    val gradientBrush = Brush.horizontalGradient(listOf(primaryColor, secondaryColor))

    var selectedType by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var showCodeField by remember { mutableStateOf(false) }
    var emailVerified by remember { mutableStateOf(false) }
    var userId by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var collegeRoll by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val departments = listOf("BSH", "CSEDS", "CSE", "IT", "ME", "AUE", "EE", "ECE", "CSEAIML")
    val semesters = (1..8).map { it.toString() }
    val facultyRoles = listOf("TEACHER", "LIBRARIAN")

    Scaffold(containerColor = Color(0xFFF5F7FA)) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFF5F7FA))
        ) {
            // ── Header ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        brush = gradientBrush,
                        shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Create Account",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Join us to get started",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // ── Card Body ────────────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {

                    // ── Account Type ─────────────────────────────────────────────
                    SectionLabel("Account Type")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ✅ weight() is valid here because we ARE inside a Row scope
                        UserTypeCard(
                            title = "Student",
                            isSelected = selectedType == "STUDENT",
                            onClick = { selectedType = "STUDENT" },
                            primaryColor = primaryColor,
                            modifier = Modifier.weight(1f)
                        )
                        UserTypeCard(
                            title = "Faculty",
                            isSelected = selectedType == "FACULTY",
                            onClick = { selectedType = "FACULTY" },
                            primaryColor = primaryColor,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Email Verification ───────────────────────────────────────
                    SectionLabel("Email Verification")
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address", color = Color.Gray) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = primaryColor)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        ),
                        enabled = !emailVerified
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!emailVerified) {
                        Button(
                            onClick = {
                                RetrofitClient.authInstance
                                    .sendEmailVerificationCode(email)
                                    .enqueue(object : Callback<ResponseBody> {
                                        override fun onResponse(
                                            call: Call<ResponseBody>,
                                            response: Response<ResponseBody>
                                        ) {
                                            if (response.isSuccessful) {
                                                showCodeField = true
                                                Toast.makeText(
                                                    context,
                                                    "Verification code sent!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Failed to send code",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }

                                        override fun onFailure(
                                            call: Call<ResponseBody>,
                                            t: Throwable
                                        ) {
                                            Toast.makeText(
                                                context,
                                                "Error: ${t.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    })
                            },
                            enabled = email.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Text("Send Code")
                        }
                    }

                    if (showCodeField && !emailVerified) {
                        Spacer(modifier = Modifier.height(12.dp))
                        // ✅ weight() is valid here because we ARE inside a Row scope
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it },
                                label = { Text("Verification Code") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = primaryColor
                                    )
                                },
                                modifier = Modifier.weight(1f),   // ✅ valid — inside Row
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color(0xFFE0E0E0)
                                )
                            )
                            Button(
                                onClick = {
                                    val body = mapOf("email" to email, "code" to code)
                                    RetrofitClient.authInstance
                                        .verifyEmailCode(body)
                                        .enqueue(object : Callback<ResponseBody> {
                                            override fun onResponse(
                                                call: Call<ResponseBody>,
                                                response: Response<ResponseBody>
                                            ) {
                                                if (response.isSuccessful) {
                                                    emailVerified = true
                                                    Toast.makeText(
                                                        context,
                                                        "Email verified successfully!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Invalid verification code",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }

                                            override fun onFailure(
                                                call: Call<ResponseBody>,
                                                t: Throwable
                                            ) {
                                                Toast.makeText(
                                                    context,
                                                    "Error: ${t.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        })
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Text("Verify")
                            }
                        }
                    }

                    if (emailVerified) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "✓ Email Verified",
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // ── Registration Form (shown only after email verified) ───────
                    if (emailVerified) {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionLabel("Personal Information")
                        Spacer(modifier = Modifier.height(12.dp))

                        StyledTextField(
                            value = userId,
                            onValueChange = { userId = it },
                            label = "User ID",
                            icon = Icons.Default.Person,
                            primaryColor = primaryColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        StyledTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = "Full Name",
                            icon = Icons.Default.Badge,
                            primaryColor = primaryColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        StyledTextField(
                            value = contact,
                            onValueChange = { contact = it },
                            label = "Contact Number",
                            icon = Icons.Default.Phone,
                            primaryColor = primaryColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // ✅ DropdownMenuBox no longer uses weight() internally
                        DropdownMenuBox(
                            options = departments,
                            selected = department,
                            onSelect = { department = it },
                            label = "Department",
                            icon = Icons.Default.Business
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Password
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = primaryColor)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Confirm Password
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm Password") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.LockReset,
                                    contentDescription = null,
                                    tint = primaryColor
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = {
                                    confirmPasswordVisible = !confirmPasswordVisible
                                }) {
                                    Icon(
                                        if (confirmPasswordVisible) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            )
                        )

                        // Faculty-specific fields
                        if (selectedType == "FACULTY") {
                            Spacer(modifier = Modifier.height(12.dp))
                            DropdownMenuBox(
                                options = facultyRoles,
                                selected = role,
                                onSelect = { role = it },
                                label = "Faculty Role",
                                icon = Icons.Default.Work
                            )
                        }

                        // Student-specific fields
                        if (selectedType == "STUDENT") {
                            Spacer(modifier = Modifier.height(12.dp))
                            collegeRoll = userId

                            StyledTextField(
                                value = collegeRoll,
                                onValueChange = { collegeRoll = it },
                                label = "College Roll Number",
                                icon = Icons.Default.School,
                                primaryColor = primaryColor
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            DropdownMenuBox(
                                options = semesters,
                                selected = semester,
                                onSelect = { semester = it },
                                label = "Current Semester",
                                icon = Icons.Default.DateRange
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Register Button ──────────────────────────────────────────
                    Button(
                        onClick = {
                            if (password != confirmPassword) {
                                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT)
                                    .show()
                                return@Button
                            }
                            loading = true

                            if (selectedType == "STUDENT") {
                                val request = mapOf(
                                    "userId" to userId,
                                    "username" to username,
                                    "department" to department,
                                    "email" to email,
                                    "contact" to contact,
                                    "password" to password,
                                    "confirmPassword" to confirmPassword,
                                    //"role" to listOf("STUDENT"),
                                    "studentId" to userId,
                                    "collegeRoll" to collegeRoll,
                                    "semester" to semester
                                )
                                RetrofitClient.authInstance
                                    .registerStudent(request)
                                    .enqueue(object : Callback<ResponseBody> {
                                        override fun onResponse(
                                            call: Call<ResponseBody>,
                                            response: Response<ResponseBody>
                                        ) {
                                            loading = false
                                            if (response.isSuccessful) {
                                                Toast.makeText(
                                                    context,
                                                    "Registration successful!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                navController.popBackStack()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Registration failed",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }

                                        override fun onFailure(
                                            call: Call<ResponseBody>,
                                            t: Throwable
                                        ) {
                                            loading = false
                                            Toast.makeText(
                                                context,
                                                "Error: ${t.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    })
                            } else {
                                val request = mapOf(
                                    "userId" to userId,
                                    "username" to username,
                                    "department" to department,
                                    "email" to email,
                                    "contact" to contact,
                                    "password" to password,
                                    "confirmPassword" to confirmPassword
                                    //"role" to listOf(role)
                                )
                                RetrofitClient.authInstance
                                    .registerFaculty(request)
                                    .enqueue(object : Callback<ResponseBody> {
                                        override fun onResponse(
                                            call: Call<ResponseBody>,
                                            response: Response<ResponseBody>
                                        ) {
                                            loading = false
                                            if (response.isSuccessful) {
                                                Toast.makeText(
                                                    context,
                                                    "Registration successful!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                navController.popBackStack()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Registration failed",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }

                                        override fun onFailure(
                                            call: Call<ResponseBody>,
                                            t: Throwable
                                        ) {
                                            loading = false
                                            Toast.makeText(
                                                context,
                                                "Error: ${t.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    })
                            }
                        },
                        enabled = emailVerified && selectedType.isNotEmpty() &&
                                userId.isNotBlank() && username.isNotBlank() &&
                                department.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Already have an account? ", color = Color.Gray)
                        Text(
                            text = "Sign In",
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

// ── Reusable helpers ─────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF333333)
    )
}

@Composable
private fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    primaryColor: Color
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = primaryColor) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = primaryColor,
            unfocusedBorderColor = Color(0xFFE0E0E0)
        )
    )
}

/**
 * Standalone dropdown — no weight() inside, so it can be called from any scope.
 * Pass [modifier] from the call site when you need width control (e.g. Modifier.weight(1f)
 * when called from inside a Row).
 */
@Composable
private fun DropdownMenuBox(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier   // ✅ caller decides width, not this composable
) {
    var expanded by remember { mutableStateOf(false) }
    val primaryColor = Color(0xFF1E88E5)

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = Color.Gray) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = primaryColor) },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color(0xFFE0E0E0)
            )
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .shadow(8.dp, RoundedCornerShape(12.dp))
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            color = if (option == selected) primaryColor else Color.Black,
                            fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

/**
 * UserTypeCard now accepts an explicit [modifier] so the caller (a Row) can
 * pass Modifier.weight(1f) — weight() is resolved in the correct RowScope.
 */
@Composable
fun UserTypeCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    modifier: Modifier = Modifier   // ✅ weight() passed in from Row, not used internally
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) primaryColor.copy(alpha = 0.1f) else Color.White
        ),
        border = if (isSelected)
            BorderStroke(2.dp, primaryColor)
        else
            BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (title == "Student") Icons.Default.School else Icons.Default.Work,
                contentDescription = null,
                tint = if (isSelected) primaryColor else Color.Gray,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = if (isSelected) primaryColor else Color.Gray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

 */

package com.mckv.attendance.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mckv.attendance.data.remote.RetrofitClient
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(navController: NavController) {
    val primaryColor = Color(0xFF1E88E5)
    val secondaryColor = Color(0xFF64B5F6)
    val gradientBrush = Brush.horizontalGradient(listOf(primaryColor, secondaryColor))

    var selectedType by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var collegeRoll by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val departments = listOf("BSH", "CSEDS", "CSE", "IT", "ME", "AUE", "EE", "ECE", "CSEAIML")
    val semesters = (1..8).map { it.toString() }
    val facultyRoles = listOf("TEACHER", "LIBRARIAN")

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        // ✅ FIX 2: Let Scaffold handle window insets so IME (keyboard) insets
        //           are respected and content scrolls above the keyboard.
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                // ✅ FIX 2: imePadding() pushes the column up when keyboard opens
                .imePadding()
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFF5F7FA))
        ) {
            // ── Header ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        brush = gradientBrush,
                        shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Create Account",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Join us to get started",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // ── Card Body ────────────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {

                    // ── Account Type ─────────────────────────────────────────────
                    SectionLabel("Account Type")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        UserTypeCard(
                            title = "Student",
                            isSelected = selectedType == "STUDENT",
                            onClick = { selectedType = "STUDENT" },
                            primaryColor = primaryColor,
                            modifier = Modifier.weight(1f)
                        )
                        UserTypeCard(
                            title = "Faculty",
                            isSelected = selectedType == "FACULTY",
                            onClick = { selectedType = "FACULTY" },
                            primaryColor = primaryColor,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (selectedType.isNotEmpty()) {
                        SectionLabel("Registration Information")
                        Spacer(modifier = Modifier.height(12.dp))

                        StyledTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = "Email Address",
                            icon = Icons.Default.Email,
                            primaryColor = primaryColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        StyledTextField(
                            value = userId,
                            onValueChange = { userId = it },
                            label = "User ID",
                            icon = Icons.Default.Person,
                            primaryColor = primaryColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        StyledTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = "Full Name",
                            icon = Icons.Default.Badge,
                            primaryColor = primaryColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        StyledTextField(
                            value = contact,
                            onValueChange = { contact = it },
                            label = "Contact Number",
                            icon = Icons.Default.Phone,
                            primaryColor = primaryColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // ✅ FIX 1: Use ExposedDropdownMenuBox — the correct M3 API
                        DropdownMenuBox(
                            options = departments,
                            selected = department,
                            onSelect = { department = it },
                            label = "Department",
                            icon = Icons.Default.Business
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Password
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = primaryColor)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Confirm Password
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm Password") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.LockReset,
                                    contentDescription = null,
                                    tint = primaryColor
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        if (confirmPasswordVisible) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            )
                        )

                        if (selectedType == "FACULTY") {
                            Spacer(modifier = Modifier.height(12.dp))
                            DropdownMenuBox(
                                options = facultyRoles,
                                selected = role,
                                onSelect = { role = it },
                                label = "Faculty Role",
                                icon = Icons.Default.Work
                            )
                        }

                        if (selectedType == "STUDENT") {
                            Spacer(modifier = Modifier.height(12.dp))
                            StyledTextField(
                                value = collegeRoll,
                                onValueChange = { collegeRoll = it },
                                label = "College Roll Number",
                                icon = Icons.Default.School,
                                primaryColor = primaryColor
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            DropdownMenuBox(
                                options = semesters,
                                selected = semester,
                                onSelect = { semester = it },
                                label = "Current Semester",
                                icon = Icons.Default.DateRange
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (password != confirmPassword) {
                                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (email.isBlank()) {
                                    Toast.makeText(context, "Email is required", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                loading = true

                                if (selectedType == "STUDENT") {
                                    val request = mapOf(
                                        "userId" to userId,
                                        "username" to username,
                                        "department" to department,
                                        "email" to email,
                                        "contact" to contact,
                                        "password" to password,
                                        "confirmPassword" to confirmPassword,
                                        "studentId" to userId,
                                        "collegeRoll" to collegeRoll,
                                        "semester" to semester
                                    )
                                    RetrofitClient.authInstance
                                        .registerStudent(request)
                                        .enqueue(object : Callback<ResponseBody> {
                                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                                loading = false
                                                val body = response.body()?.string()
                                                val json = JSONObject(body)
                                                val message = json.getString("message")
                                                if (response.isSuccessful) {
                                                    Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                                                    navController.popBackStack()
                                                } else {
                                                    Toast.makeText(context, "$message", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                                loading = false
                                                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        })
                                } else {
                                    val request = mapOf(
                                        "userId" to userId,
                                        "username" to username,
                                        "department" to department,
                                        "email" to email,
                                        "contact" to contact,
                                        "password" to password,
                                        "confirmPassword" to confirmPassword,
                                        //"role" to role
                                    )
                                    RetrofitClient.authInstance
                                        .registerFaculty(request)
                                        .enqueue(object : Callback<ResponseBody> {
                                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                                val body = response.body()?.string()
                                                val json = JSONObject(body)
                                                val message = json.getString("message")
                                                loading = false
                                                if (response.isSuccessful) {
                                                    Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                                                    navController.popBackStack()
                                                } else {
                                                    println("Registration failed\n******\n******\n******$response")
                                                }
                                            }
                                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                                loading = false
                                                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        })
                                }
                            },
                            enabled = !loading &&
                                    selectedType.isNotEmpty() &&
                                    email.isNotBlank() &&
                                    userId.isNotBlank() &&
                                    username.isNotBlank() &&
                                    department.isNotBlank() &&
                                    password.isNotBlank() &&
                                    confirmPassword.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            if (loading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            } else {
                                Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                    } else {
                        Spacer(modifier = Modifier.height(24.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8))
                        ) {
                            Text(
                                text = "Please select Student or Faculty to continue",
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center,
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Already have an account? ", color = Color.Gray)
                        Text(
                            text = "Sign In",
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { navController.popBackStack() }
                        )
                    }

                    // ✅ FIX 2: Extra bottom padding so last items aren't clipped
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// ── Reusable helpers ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333))
}

@Composable
private fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    primaryColor: Color
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = primaryColor) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = primaryColor,
            unfocusedBorderColor = Color(0xFFE0E0E0)
        )
    )
}

// ✅ FIX 1: Replaced the broken Box+clickable approach with ExposedDropdownMenuBox
//           which is the correct Material 3 API for dropdown fields.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuBox(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val primaryColor = Color(0xFF1E88E5)
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = Color.Gray) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = primaryColor) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),          // ← this is what connects the field to the menu
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color(0xFFE0E0E0)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            color = if (option == selected) primaryColor else Color.Black,
                            fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun UserTypeCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) primaryColor.copy(alpha = 0.1f) else Color.White
        ),
        border = if (isSelected) BorderStroke(2.dp, primaryColor) else BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (title == "Student") Icons.Default.School else Icons.Default.Work,
                contentDescription = null,
                tint = if (isSelected) primaryColor else Color.Gray,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = if (isSelected) primaryColor else Color.Gray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}