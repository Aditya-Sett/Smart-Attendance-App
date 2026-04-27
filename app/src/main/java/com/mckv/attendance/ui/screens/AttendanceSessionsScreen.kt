package com.mckv.attendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mckv.attendance.data.local.SessionManager
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import com.mckv.attendance.data.remote.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Calendar
import java.util.Date

// Color palette
val PrimaryPurple = Color(0xFF6C63FF)
val PrimaryDarkPurple = Color(0xFF4A44CC)
val SecondaryOrange = Color(0xFFFF6584)
val SuccessGreen = Color(0xFF4CAF50)
val SurfaceGray = Color(0xFFF8F9FA)
val TextDark = Color(0xFF2C3E50)
val TextLight = Color(0xFF7F8C8D)

fun convertToIST(gmtTime: String): String {
    val inputFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
    inputFormat.timeZone = TimeZone.getTimeZone("GMT")

    val date = inputFormat.parse(gmtTime)

    val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
    outputFormat.timeZone = TimeZone.getTimeZone("Asia/Kolkata")

    return outputFormat.format(date!!)
}

fun getTodayDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}

fun getYesterdayDate(): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -1)
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(calendar.time)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceSessionsScreen(navController: NavController) {

    val context = LocalContext.current
    val teacherId = SessionManager.userDetails?.userId ?: "Unknown"

    var selectedDate by remember { mutableStateOf(getTodayDate()) }
    var sessions by remember { mutableStateOf(listOf<JSONObject>()) }
    var isLoading by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDate) {
        isLoading = true
        sessions = fetchSessions(teacherId, selectedDate)
        isLoading = false
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = PrimaryPurple)
                }
            }
        ) {
            // Custom date picker implementation would go here
            // For simplicity, we're showing a basic one
            BasicDatePicker(
                onDateSelected = { date ->
                    selectedDate = date
                    showDatePicker = false
                }
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SurfaceGray
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header Section with Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(PrimaryPurple, PrimaryDarkPurple),
                            start = Offset(0f, 0f),
                            end = Offset(1000f, 1000f)
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = "Attendance Sessions",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Track and manage your attendance records",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Date Selector Section
            DateSelector(
                selectedDate = selectedDate,
                onDateChange = { selectedDate = it },
                onCustomDate = { showDatePicker = true }
            )

            // Content Section
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = PrimaryPurple,
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Add an empty state icon here if you have one
                        Text(
                            text = "📋",
                            fontSize = 64.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No sessions found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextLight
                        )
                        Text(
                            text = "Try selecting a different date",
                            fontSize = 14.sp,
                            color = TextLight
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sessions) { session ->
                        AttendanceCard(session) {
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("sessionData", session.toString())
                            navController.navigate("session_details")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceCard(session: JSONObject, onClick: () -> Unit) {

    val subject = session.getString("subject")
    val department = session.getString("department")
    val sem = session.getString("sem")
    val academicYear = session.getString("academicYear")
    val actualgeneratedAt = session.getString("generatedAt")
    println("actualgeneratedAt: $actualgeneratedAt")
    val generatedAt = convertToIST(actualgeneratedAt)
    println("generatedAt: $generatedAt")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Subject initial or icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(PrimaryPurple, PrimaryDarkPurple)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = subject.take(2).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Middle - Session details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = subject,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Chip(
                        text = department,
                        color = PrimaryPurple.copy(alpha = 0.1f),
                        textColor = PrimaryPurple
                    )
                    Chip(
                        text = "Sem $sem",
                        color = SecondaryOrange.copy(alpha = 0.1f),
                        textColor = SecondaryOrange
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "📅",
                        fontSize = 11.sp
                    )
                    Text(
                        text = generatedAt,
                        fontSize = 11.sp,
                        color = TextLight
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "📚 $academicYear",
                    fontSize = 11.sp,
                    color = TextLight
                )
            }

            // Right side - Arrow icon
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Details",
                tint = PrimaryPurple,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun Chip(
    text: String,
    color: Color,
    textColor: Color
) {
    Surface(
        modifier = Modifier,
        shape = RoundedCornerShape(8.dp),
        color = color
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun DateSelector(
    selectedDate: String,
    onDateChange: (String) -> Unit,
    onCustomDate: () -> Unit
) {
    val formattedDate = try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = inputFormat.parse(selectedDate)
        val outputFormat = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
        outputFormat.format(date!!)
    } catch (e: Exception) {
        selectedDate
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Selected date display
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = formattedDate,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDark
                    )
                }
                IconButton(
                    onClick = onCustomDate,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text(
                        text = "📅",
                        fontSize = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            QuickDateButton(
                text = "Today",
                icon = Icons.Default.Today,
                isSelected = selectedDate == getTodayDate(),
                onClick = { onDateChange(getTodayDate()) },
                modifier = Modifier.weight(1f)
            )

            QuickDateButton(
                text = "Yesterday",
                icon = Icons.Default.History,
                isSelected = selectedDate == getYesterdayDate(),
                onClick = { onDateChange(getYesterdayDate()) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun QuickDateButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PrimaryPurple else Color.White,
            contentColor = if (isSelected) Color.White else PrimaryPurple
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicDatePicker(
    onDateSelected: (String) -> Unit
) {
    val datePickerState = rememberDatePickerState()
    val selectedDate = datePickerState.selectedDateMillis?.let {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        format.format(it)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false
        )

        Spacer(modifier = Modifier.height(1.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { onDateSelected(getTodayDate()) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    selectedDate?.let { onDateSelected(it) }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryPurple
                )
            ) {
                Text("Confirm")
            }
        }
    }
}

suspend fun fetchSessions(
    teacherId: String,
    date: String
): List<JSONObject> {

    return try {
        val json = JSONObject().apply {
            put("teacherId", teacherId)
            put("date", date)
        }

        val body = json.toString()
            .toRequestBody("application/json".toMediaTypeOrNull())

        val response = RetrofitClient.analysisInstance.getAttendanceSessions(body)

        if (response.isSuccessful) {
            val resString = response.body()?.string()
            val jsonObj = JSONObject(resString ?: "{}")

            val dataArray = jsonObj.getJSONArray("data")

            val list = mutableListOf<JSONObject>()

            for (i in 0 until dataArray.length()) {
                list.add(dataArray.getJSONObject(i))
            }

            list
        } else {
            emptyList()
        }

    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}