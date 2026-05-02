package com.mckv.attendance.ui.screens.student

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.mckv.attendance.ui.components.common.CommonTopBar
import com.mckv.attendance.utils.interactionDetection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentScreen(
    navController: NavHostController,
    studentViewModel: StudentViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by studentViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Bluetooth Broadcast Receiver ──────────────────────────────────────────
    val bluetoothReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    studentViewModel.onBluetoothStateChanged(state)
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothReceiver, filter)
        studentViewModel.checkBluetoothStatus(context, activity)
        onDispose { context.unregisterReceiver(bluetoothReceiver) }
    }

    // ── Snackbar ──────────────────────────────────────────────────────────────
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            studentViewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier.interactionDetection(),
        topBar = {
            Column {
                CommonTopBar(title = "Smart Attendance", navController = navController)
                StudentTopBanner(
                    studentId = uiState.studentId,
                    department = uiState.department,
                    bluetoothChecking = uiState.bluetoothChecking,
                    bluetoothEnabled = uiState.bluetoothEnabled,
                    bluetoothSupported = uiState.bluetoothSupported,
                    onRefresh = { studentViewModel.checkBluetoothStatus(context, activity) }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BluetoothStatusCard(
                checking = uiState.bluetoothChecking,
                supported = uiState.bluetoothSupported,
                enabled = uiState.bluetoothEnabled,
                onEnableBluetooth = { studentViewModel.requestBluetoothEnable(activity) },
                onRefresh = { studentViewModel.checkBluetoothStatus(context, activity) }
            )

            Spacer(Modifier.height(4.dp))

            StudentActionButtons(
                bluetoothChecking = uiState.bluetoothChecking,
                bluetoothEnabled = uiState.bluetoothEnabled, // Add this line
                onSchedule = { navController.navigate("schedule") },
                onHistory = { navController.navigate("attendance_summary") },
                onGiveAttendance = { navController.navigate("give_attendance") }
            )

            if (!uiState.bluetoothEnabled && !uiState.bluetoothChecking && uiState.bluetoothSupported) {
                InfoNote(
                    text = "You can still view schedule and history, but attendance marking requires Bluetooth."
                )
            }
        }
    }
}

// ─── Top Banner ───────────────────────────────────────────────────────────────

@Composable
private fun StudentTopBanner(
    studentId: String,
    department: String,
    bluetoothChecking: Boolean,
    bluetoothEnabled: Boolean,
    bluetoothSupported: Boolean,
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF1565C0), Color(0xFF1E88E5))
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Welcome, $studentId",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp
                )
                Text(
                    text = "Dept: $department",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 15.sp
                )
            }
            BluetoothPill(
                checking = bluetoothChecking,
                enabled = bluetoothEnabled,
                supported = bluetoothSupported,
                onRefresh = onRefresh
            )
        }
    }
}

// ─── Bluetooth Pill Indicator ─────────────────────────────────────────────────

@Composable
private fun BluetoothPill(
    checking: Boolean,
    enabled: Boolean,
    supported: Boolean,
    onRefresh: () -> Unit
) {
    val dotColor = when {
        checking -> Color(0xFFFFEB3B)
        enabled -> Color(0xFF66BB6A)
        else -> Color(0xFFEF5350)
    }
    val label = when {
        checking -> "Checking..."
        !supported -> "Not Supported"
        enabled -> "BT ON"
        else -> "BT OFF"
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f, targetValue = 1.2f,
                animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                label = "dot"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            if (!checking && !enabled && supported) {
                IconButton(onClick = onRefresh, modifier = Modifier.size(14.dp)) {
                    Icon(Icons.Default.Refresh, null, tint = Color.White)
                }
            }
        }
    }
}

// ─── Bluetooth Status Card ────────────────────────────────────────────────────

@Composable
private fun BluetoothStatusCard(
    checking: Boolean,
    supported: Boolean,
    enabled: Boolean,
    onEnableBluetooth: () -> Unit,
    onRefresh: () -> Unit
) {
    AnimatedContent(
        targetState = Triple(checking, supported, enabled),
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "btCard"
    ) { (isChecking, isSupported, isEnabled) ->
        when {
            isChecking -> Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text("Checking Bluetooth status...", color = Color(0xFF616161))
                }
            }

            !isSupported -> StatusCard(
                emoji = "⚠️",
                title = "Bluetooth Not Supported",
                subtitle = "Attendance marking will not work on this device.",
                containerColor = Color(0xFFFFEBEE),
                textColor = Color(0xFFC62828)
            )

            !isEnabled -> Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(), // Ensure the column takes full width to allow centering
                    horizontalAlignment = Alignment.CenterHorizontally ,// Centers the items horizontally
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "⚠️ Bluetooth Required",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF795548),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center //
                    )
                    Text(
                        "Enable Bluetooth to mark attendance.",
                        fontSize = 13.sp,
                        color = Color(0xFF795548),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center //
                    )
                    Button(
                        onClick = onEnableBluetooth,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF795548)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Enable Bluetooth")
                    }
                }
            }

            else -> StatusCard(
                emoji = "✓",
                title = "Ready for Attendance",
                subtitle = "Bluetooth is ON. You'll be notified when a teacher starts attendance.",
                containerColor = Color(0xFFE8F5E9),
                textColor = Color(0xFF2E7D32),
                trailingIcon = {
                    IconButton(onClick = onRefresh, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Refresh, null, tint = Color(0xFF2E7D32))
                    }
                }
            )
        }
    }
}

@Composable
private fun StatusCard(
    emoji: String,
    title: String,
    subtitle: String,
    containerColor: Color,
    textColor: Color,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(title, fontWeight = FontWeight.SemiBold, color = textColor)
                }
                Spacer(Modifier.height(4.dp))
                Text(subtitle, fontSize = 12.sp, color = textColor.copy(alpha = 0.8f))
            }
            trailingIcon?.invoke()
        }
    }
}

// ─── Action Buttons ───────────────────────────────────────────────────────────

@Composable
private fun StudentActionButtons(
    bluetoothChecking: Boolean,
    bluetoothEnabled: Boolean,
    onSchedule: () -> Unit,
    onHistory: () -> Unit,
    onGiveAttendance: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ActionButton(
            label = "📅  View Schedule",
            onClick = onSchedule,
            enabled = !bluetoothChecking,
            containerColor = Color(0xFF1E88E5)
        )
        ActionButton(
            label = "📊  Attendance History",
            onClick = onHistory,
            enabled = !bluetoothChecking,
            containerColor = Color(0xFF43A047)
        )
        ActionButton(
            label = "✋  Give Attendance",
            onClick = onGiveAttendance,
            enabled = !bluetoothChecking && bluetoothEnabled,
            containerColor = Color(0xFF7B1FA2)
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    containerColor: Color
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

// ─── Info Note ────────────────────────────────────────────────────────────────

@Composable
private fun InfoNote(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ℹ️", fontSize = 13.sp)
            Text(text, fontSize = 12.sp, color = Color(0xFF757575))
        }
    }
}