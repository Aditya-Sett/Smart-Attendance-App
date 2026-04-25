package com.mckv.attendance.ui.screens.take_attendance

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mckv.attendance.R
import com.mckv.attendance.data.local.SessionManager
import com.mckv.attendance.data.remote.api.AttendanceCodeModel
import com.mckv.attendance.utils.DepartmentAutoComplete
import com.mckv.attendance.utils.SubjectAutoComplete
import com.mckv.attendance.utils.getCurrentISTMillis
import kotlinx.coroutines.delay
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeAttendanceScreen2(navController: NavController, viewModel: TakeAttendanceViewModel= viewModel() ){

    val context = LocalContext.current

    // OBSERVE THE STATE VIEWMODEL
    val state by viewModel.uiState.collectAsState()
    val activeList by viewModel.activeAttendanceList.collectAsState()

    //FETCHING THE TEACHER ID THAT STORED AFTER LOGIN IN SESSION MANAGER
    val teacherId = SessionManager.userDetails?.userId ?: "Unknown"

    //LOCAL VARIABLES THAT TO BE AUTO FIL
    var department by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }

    var expanded by remember { mutableStateOf(false) }
    val semOptions = listOf("1st", "2nd", "3rd", "4th","5th", "6th", "7th", "8th")
    var responseMessage by remember { mutableStateOf<String?>(null) }
    var responseList by remember { mutableStateOf(mutableListOf<String>()) }
    var bluetoothUuid by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    // --- ADD THIS BLOCK ---
    // Monitor Bluetooth State to clear UI instantly if BT is turned off
    androidx.compose.runtime.DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                if (intent?.action == android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(
                        android.bluetooth.BluetoothAdapter.EXTRA_STATE,
                        android.bluetooth.BluetoothAdapter.ERROR
                    )
                    // If BT is OFF or Turning OFF, and we have an active card
                    if ((state == android.bluetooth.BluetoothAdapter.STATE_OFF ||
                                state == android.bluetooth.BluetoothAdapter.STATE_TURNING_OFF) &&
                        activeList.isNotEmpty()) {

                        viewModel.handleBluetoothDisabled()
                    }
                }
            }
        }
        val filter = android.content.IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(receiver, filter)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    //LAUNCHER FOR BLUETOOTH PERMISSION FRO USER
    // 1. Update the launcher to handle multiple permissions
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val advertised = permissions[android.Manifest.permission.BLUETOOTH_ADVERTISE] ?: false
        val connected = permissions[android.Manifest.permission.BLUETOOTH_CONNECT] ?: false

        if (advertised && connected) {
            android.widget.Toast.makeText(context, "Permissions Granted! Click again to generate.", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(context, "Bluetooth permissions are required for broadcasting", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    //TRIGGER THE AUTO FILL ONCE ON START EVEN ON ROTATE
    //LAUNCHED EFFECT IS A COROUTINE RUN CODE DETACHED FROM UI WORK SAFELY
    //UNIT MEAN IT START ONCE AND JUST LISTEN
    LaunchedEffect(Unit) {
        viewModel.loadSavedActiveCode()

        if(state is AttendanceUiState.Idle){
            viewModel.fetchCurrentClass(teacherId)
        }
    }

    //UPDATE LOCAL FORM FIELD WHEN STATE CHANG: AUTO FILL
    //STATE MEAN IT REACT EVERY TIME STATE CHANGE
    LaunchedEffect(state) {
        if (state is AttendanceUiState.FetchCurrentClassSuccess) {
            val data = state as AttendanceUiState.FetchCurrentClassSuccess

            department = data.department
            subject = data.subject
            semester = data.semester
        }
    }

    //RESPONSIBLE FOR TOAST MESSAGE
    LaunchedEffect(Unit) {
        viewModel.events.collect { message->
            Toast.makeText(context,message, Toast.LENGTH_SHORT).show()
        }
    }

    //UI DESIGN
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teacher Panel") },
//                navigationIcon = {
//                    IconButton(onClick = { /* TODO */ }) {
//                        Icon(Icons.Default.Menu, contentDescription = "Menu")
//                    }
//                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {

            //CHECK STATE
            if (state is AttendanceUiState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    //TEACHER ID FIELD
                    OutlinedTextField(
                        value = teacherId,
                        onValueChange = {},
                        label = { Text("Teacher ID") },
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    //DEPARTMENT FIELD
                    DepartmentAutoComplete(
                        department = department,
                        onDepartmentChange = { department = it }
                    )

                    //SUBJECT FIELD
                    SubjectAutoComplete(
                        subject = subject,
                        onSubjectChange = { subject = it }
                    )

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = semester,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Semester") },
                            leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            semOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        semester = option
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (department.isBlank() || subject.isBlank() || semester.isBlank()) {
                                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // 2. CHECK PERMISSION BEFORE CALLING BLUETOOTH LOGIC
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                val permissionsToRequest = arrayOf(
                                    android.Manifest.permission.BLUETOOTH_ADVERTISE,
                                    android.Manifest.permission.BLUETOOTH_CONNECT
                                )

                                val allGranted = permissionsToRequest.all {
                                    androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                }

                                if (!allGranted) {
                                    permissionLauncher.launch(permissionsToRequest)
                                    return@Button
                                }
                            }

                            // 1. Get WiFi Fingerprint (Assuming you have the helper function available)
                            // val wifiFingerprint = buildWifiFingerprint(context)
                            val wifiFingerprint = JSONArray() // Placeholder

                            // Check Bluetooth first
                            checkBluetoothAndGenerate(context) {
                                // This only runs if Bluetooth is ON
                                viewModel.generateAttendanceCode(
                                    teacherId = teacherId,
                                    department = department,
                                    subject = subject,
                                    wifiFingerprint = JSONArray(),
                                    sem = semester
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),

                        enabled= (state is AttendanceUiState.Idle || state is AttendanceUiState.FetchCurrentClassSuccess)
                                && state !is AttendanceUiState.LoadingGenerateAttendance
                                && activeList.isEmpty()  // ← ADD THIS
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        if(state is AttendanceUiState.LoadingGenerateAttendance){
                            // Show a small progress indicator inside the button
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Generating...")
                        }else{
                            Text(if (state is AttendanceUiState.ActiveSessionState) "Session in Progress" else "Generate Attendance Code")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active Code Cards List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(activeList.size) { index ->
                    val attendanceData = activeList[index]
                    // Reusing your CompactCodeCard
                    CompactCodeCard(
                        attendanceData,
                        state,
                        onClose = {
                            viewModel.deleteCode(attendanceData)
                        },
                        onTimerFinished = {
                            // This will be called when the timer reaches 00:00
                            viewModel.handleSessionExpiration()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CompactCodeCard(
    response: AttendanceCodeModel,
    uiState: AttendanceUiState,
    onClose: () -> Unit,
    onTimerFinished: () -> Unit
) {
    val code = response.code
    val department = response.department
    val subject = response.subject
    val generatedAt = response.generatedAt

    val expiresAt = response.expiresAt // e.g. "23-04-2026 06:40:44 PM"
    var remainingTime by remember { mutableStateOf(0L) }

    // --- UPDATE THIS: Use expiresAt as the Key ---
    LaunchedEffect(expiresAt) {
        val format = SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.getDefault())
        val expTime = format.parse(expiresAt)?.time ?: 0L
        val currentIST = getCurrentISTMillis()

        // Recalculate remaining time whenever the expiration string changes
        remainingTime = maxOf((expTime - currentIST) / 1000, 0)
    }

    // Timer countdown logic stays the same
    LaunchedEffect(remainingTime) {
        if (remainingTime > 0) {
            delay(1000)
            remainingTime--
        } else if (remainingTime == 0L) {
            onTimerFinished()
        }
    }

    val timerText = String.format("%02d:%02d", remainingTime / 60, remainingTime % 60)

    Card(
        modifier = Modifier
            .padding(12.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                )

                // LOGIC: Show Spinner if Loading, otherwise show Icon/Expired text
                if (uiState is AttendanceUiState.LoadingDeleteCode) {
                    // Show small spinner while deleting
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).padding(end = 8.dp),
                        color = Color(0xFF1976D2),
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (remainingTime <= 0) {
                            Text(
                                text = "Expired",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold, // Increased weight for better visibility
                                    fontSize = 22.sp,                // Increased font size
                                    color = Color.Red
                                ),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }else{
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.close_circle),
                                    contentDescription = "Close",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("$department | $subject", fontWeight = FontWeight.Medium)
                }

                if (remainingTime > 0) {
                    Text(
                        text = timerText,
                        color = if (remainingTime < 20) Color.Red else Color(0xFF388E3C),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun checkBluetoothAndGenerate(
    context: android.content.Context,
    onEnabled: () -> Unit
) {
    val bluetoothManager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
    val adapter = bluetoothManager.adapter

    if (adapter == null) {
        Toast.makeText(context, "Bluetooth not supported",Toast.LENGTH_SHORT).show()
    } else if (!adapter.isEnabled) {
        // Launch intent to turn on Bluetooth
        val enableBtIntent = android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
        context.startActivity(enableBtIntent)
    } else {
        onEnabled()
    }
}