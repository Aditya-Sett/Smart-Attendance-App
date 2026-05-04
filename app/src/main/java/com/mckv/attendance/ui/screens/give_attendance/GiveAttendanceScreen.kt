package com.mckv.attendance.ui.screens.give_attendance

import androidx.compose.ui.graphics.graphicsLayer

import android.app.Activity
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.mckv.attendance.ui.components.common.CommonTopBar
import com.mckv.attendance.utils.formatTimeRemaining
import com.mckv.attendance.utils.interactionDetection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiveAttendanceScreen(
    navController: NavHostController,
    viewModel: GiveAttendanceViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.startPollingAndBle(context)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopAll() }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(uiState.submissionResult) {
        if (uiState.submissionResult is SubmissionResult.Success) {
            kotlinx.coroutines.delay(1500)
            navController.popBackStack()
        }
    }

    Scaffold(
        modifier = Modifier.interactionDetection(),
        topBar = {
            CommonTopBar(title = "Give Attendance", navController = navController)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // ── Title ──────────────────────────────────────────────────────────
            Text(
                text = "Mark Your Attendance",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A237E)
            )
            Text(
                text = "Both conditions must be met to submit",
                fontSize = 13.sp,
                color = Color(0xFF757575),
                textAlign = TextAlign.Center
            )

            // ── Subject + Timer ────────────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.isCodeAvailable && uiState.activeSubject != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                SubjectTimerCard(
                    subject = uiState.activeSubject ?: "",
                    timeLeftMillis = uiState.timeLeftMillis,
                    isExpired = uiState.isExpired
                )
            }

            // ── Expired Banner ─────────────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.isExpired,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ExpiredBanner(
                    subject = uiState.activeSubject ?: "",
                    onDismiss = {
                        viewModel.resetExpiry()
                        navController.popBackStack()
                    }
                )
            }

            // ── Attendance Form ────────────────────────────────────────────────
            if (!uiState.isExpired) {
                AttendanceFormCard(
                    inputCode = uiState.inputCode,
                    onCodeChange = viewModel::onInputCodeChanged,
                    isEnabled = uiState.isCodeAvailable && uiState.isTeacherNearby,
                    isSubmitting = uiState.isSubmitting,
                    onSubmit = { viewModel.submitAttendance(context) },
                    submissionResult = uiState.submissionResult,
                    isCodeAvailable = uiState.isCodeAvailable,
                    isTeacherNearby = uiState.isTeacherNearby,
                    isPolling = uiState.isPollingCode
                )
            }

            // ── Failure message ────────────────────────────────────────────────
            AnimatedVisibility(visible = uiState.submissionResult is SubmissionResult.Failure) {
                val msg = (uiState.submissionResult as? SubmissionResult.Failure)?.message ?: ""
                FailureCard(message = msg, onDismiss = viewModel::clearSubmissionResult)
            }

            // ── Waiting hint ───────────────────────────────────────────────────
            if (!uiState.isCodeAvailable && !uiState.isExpired) {
                WaitingHint()
            }
        }
    }
}

// ─── Subject + Timer Card ─────────────────────────────────────────────────────

@Composable
private fun SubjectTimerCard(
    subject: String,
    timeLeftMillis: Long?,
    isExpired: Boolean
) {
    val minutes = ((timeLeftMillis ?: 0) / 60000).toInt()
    val seconds = (((timeLeftMillis ?: 0) % 60000) / 1000).toInt()
    val isUrgent = minutes < 1 && !isExpired

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUrgent) Color(0xFFFFEBEE) else Color(0xFFE3F2FD)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Subject", fontSize = 11.sp, color = Color(0xFF78909C))
                Text(
                    subject,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A237E)
                )
            }
            if (timeLeftMillis != null && timeLeftMillis > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Time Left", fontSize = 11.sp, color = Color(0xFF78909C))
                    Text(
                        text = formatTimeRemaining(minutes, seconds),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUrgent) Color(0xFFD32F2F) else Color(0xFF1565C0)
                    )
                }
            }
        }
    }
}

// ─── Expired Banner ───────────────────────────────────────────────────────────

@Composable
private fun ExpiredBanner(subject: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("⏰", fontSize = 36.sp)
            Text(
                "TIME'S UP!",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFC62828)
            )
            Text(
                "The attendance window for $subject has closed.",
                fontSize = 13.sp,
                color = Color(0xFF757575),
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("OK, Go Back")
            }
        }
    }
}

// ─── Attendance Form Card ─────────────────────────────────────────────────────

@Composable
private fun AttendanceFormCard(
    inputCode: String,
    onCodeChange: (String) -> Unit,
    isEnabled: Boolean,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
    submissionResult: SubmissionResult?,
    isCodeAvailable: Boolean,
    isTeacherNearby: Boolean,
    isPolling: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isEnabled) 6.dp else 1.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) Color.White else Color(0xFFF5F5F5)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Title row with mini bulbs top-right ───────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Enter Attendance Code",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = if (isEnabled) Color(0xFF1A237E) else Color(0xFF9E9E9E)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiniBulb(
                        isActive = isCodeAvailable,
                        isChecking = isPolling && !isCodeAvailable,
                        tooltip = "Code"
                    )
                    MiniBulb(
                        isActive = isTeacherNearby,
                        isChecking = isCodeAvailable && !isTeacherNearby,
                        tooltip = "BLE"
                    )
                }
            }

            if (!isEnabled) {
                Text(
                    "🔒 Waiting for both conditions above to be met",
                    fontSize = 12.sp,
                    color = Color(0xFF9E9E9E),
                    textAlign = TextAlign.Center
                )
            }

            OutlinedTextField(
                value = inputCode,
                onValueChange = onCodeChange,
                label = { Text("4-digit Code") },
                singleLine = true,
                enabled = isEnabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1E88E5),
                    unfocusedBorderColor = Color(0xFFBDBDBD),
                    disabledBorderColor = Color(0xFFE0E0E0),
                    disabledTextColor = Color(0xFFBDBDBD),
                    disabledLabelColor = Color(0xFFBDBDBD)
                )
            )

            // Code length indicator dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                repeat(4) { i ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (i < inputCode.length) Color(0xFF1E88E5)
                                else Color(0xFFE0E0E0)
                            )
                    )
                }
            }

            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = isEnabled && inputCode.length == 4 && !isSubmitting,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7B1FA2),
                    disabledContainerColor = Color(0xFFE0E0E0)
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else if (submissionResult is SubmissionResult.Success) {
                    Icon(Icons.Default.Check, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Submitted!", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Text("Submit Attendance", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─── Mini Bulb ────────────────────────────────────────────────────────────────

@Composable
private fun MiniBulb(
    isActive: Boolean,
    isChecking: Boolean,
    tooltip: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "miniBulb_$tooltip")

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse),
        label = "glow"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "pulse"
    )

    val bulbColor = when {
        isActive   -> Color(0xFF4CAF50)
        isChecking -> Color(0xFFFFB300)
        else       -> Color(0xFFEF5350)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Soft glow ring when active
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(bulbColor.copy(alpha = glowAlpha * 0.3f))
                )
            }
            Box(
                modifier = Modifier
                    .size(if (isChecking) (12 * pulseScale).dp else 12.dp)
                    .clip(CircleShape)
                    .background(bulbColor)
                    .border(1.dp, bulbColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(8.dp),
                        color = Color.White,
                        strokeWidth = 1.dp
                    )
                }
            }
        }
        Text(
            text = tooltip,
            fontSize = 8.sp,
            color = bulbColor,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─── Failure Card ─────────────────────────────────────────────────────────────

@Composable
private fun FailureCard(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(message, color = Color(0xFFC62828), fontSize = 13.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = Color(0xFFC62828), fontSize = 12.sp)
            }
        }
    }
}

// ─── Waiting Hint ─────────────────────────────────────────────────────────────

@Composable
private fun WaitingHint() {
    val infiniteTransition = rememberInfiniteTransition(label = "waiting")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "alpha"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("📡", fontSize = 28.sp, modifier = Modifier.graphicsLayer { this.alpha = alpha })
        Text(
            "Scanning for active attendance session...",
            fontSize = 13.sp,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center
        )
        Text(
            "Make sure you are in class and Bluetooth is ON",
            fontSize = 11.sp,
            color = Color(0xFFBDBDBD),
            textAlign = TextAlign.Center
        )
    }
}