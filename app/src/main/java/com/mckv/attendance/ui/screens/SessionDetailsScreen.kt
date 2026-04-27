package com.mckv.attendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mckv.attendance.data.local.SessionManager
import com.mckv.attendance.data.remote.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import com.mckv.attendance.ui.screens.convertToIST

// Modern color palette
object AttendanceColors {
    val Primary = Color(0xFF1A237E)
    val PrimaryLight = Color(0xFF534bae)
    val PrimaryDark = Color(0xFF000051)
    val Accent = Color(0xFF00BCD4)
    val Success = Color(0xFF4CAF50)
    val Error = Color(0xFFF44336)
    val Warning = Color(0xFFFF9800)
    val Background = Color(0xFFF5F5F5)
    val Surface = Color.White
    val TextPrimary = Color(0xFF212121)
    val TextSecondary = Color(0xFF757575)
    val Divider = Color(0xFFE0E0E0)
    val PresentBg = Color(0xFFE8F5E9)
    val AbsentBg = Color(0xFFFFEBEE)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailsScreen(navController: NavController) {
    val teacherId = SessionManager.userDetails?.userId ?: "Unknown"

    val sessionString = navController
        .previousBackStackEntry
        ?.savedStateHandle
        ?.get<String>("sessionData")

    val sessionJson = JSONObject(sessionString ?: "{}")
    val generatedAt = sessionJson.getString("generatedAt")
    val sessionName = sessionJson.optString("sessionName", "Attendance Session")

    var studentList by remember { mutableStateOf(listOf<StudentAttendance>()) }
    var filteredList by remember { mutableStateOf(listOf<StudentAttendance>()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<AttendanceFilter?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val rawData = fetchAttendanceSummary(teacherId, generatedAt)
        studentList = rawData.map {
            StudentAttendance(
                studentId = it.getString("studentId"),
                roll = it.getString("collegeRoll"),
                name = it.getString("username"),
                status = it.getString("status")
            )
        }
        filteredList = studentList
        isLoading = false
    }

    LaunchedEffect(searchQuery, selectedFilter, studentList) {
        delay(300)
        var result = studentList

        if (searchQuery.isNotBlank()) {
            result = result.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.roll.contains(searchQuery, ignoreCase = true) ||
                        it.studentId.contains(searchQuery, ignoreCase = true)
            }
        }

        when (selectedFilter) {
            AttendanceFilter.PRESENT -> result = result.filter { it.status == "present" }
            AttendanceFilter.ABSENT -> result = result.filter { it.status == "absent" }
            null -> {}
        }

        filteredList = result
    }

    val presentCount = studentList.count { it.status == "present" }
    val absentCount = studentList.count { it.status == "absent" }
    val totalCount = studentList.size
    val attendancePercentage = if (totalCount > 0) (presentCount * 100f / totalCount) else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = sessionName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = convertToIST(generatedAt) ,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AttendanceColors.Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AttendanceColors.Background)
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AttendanceColors.Primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading attendance data...", color = AttendanceColors.TextSecondary)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Summary Cards Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            title = "Present",
                            count = presentCount,
                            color = AttendanceColors.Success,
                            icon = Icons.Default.CheckCircle,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "Absent",
                            count = absentCount,
                            color = AttendanceColors.Error,
                            icon = Icons.Default.Cancel,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "Total",
                            count = totalCount,
                            color = AttendanceColors.Primary,
                            icon = Icons.Default.People,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Attendance Percentage Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AttendanceColors.Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Attendance Rate",
                                    fontSize = 12.sp,
                                    color = AttendanceColors.TextSecondary
                                )
                                Text(
                                    text = String.format("%.1f%%", attendancePercentage),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (attendancePercentage >= 75) AttendanceColors.Success
                                    else if (attendancePercentage >= 50) AttendanceColors.Warning
                                    else AttendanceColors.Error
                                )
                            }
                            LinearProgressIndicator(
                                progress = attendancePercentage / 100f,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .padding(start = 16.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (attendancePercentage >= 75) AttendanceColors.Success
                                else if (attendancePercentage >= 50) AttendanceColors.Warning
                                else AttendanceColors.Error,
                                trackColor = AttendanceColors.Divider
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Search and Filter Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(text = "Search by name, roll, Id", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AttendanceColors.Primary,
                                unfocusedBorderColor = AttendanceColors.Divider
                            ),
                            singleLine = true
                        )

                        BadgedBox(badge = {
                            if (selectedFilter != null) {
                                Badge(containerColor = AttendanceColors.Primary) {
                                    Text("1")
                                }
                            }
                        }) {
                            FilterChip(
                                selected = showFilterMenu,
                                onClick = { showFilterMenu = !showFilterMenu },
                                label = {
                                    Text(
                                        when (selectedFilter) {
                                            AttendanceFilter.PRESENT -> "Present"
                                            AttendanceFilter.ABSENT -> "Absent"
                                            null -> "Filter"
                                        }
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (selectedFilter == null) Icons.Default.FilterList
                                        else Icons.Default.FilterListOff,
                                        contentDescription = "Filter"
                                    )
                                },
                                modifier = Modifier.height(56.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AttendanceColors.Primary.copy(alpha = 0.1f),
                                    selectedLabelColor = AttendanceColors.Primary
                                )
                            )
                        }
                    }

                    // Filter Dropdown Menu
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false },
                        modifier = Modifier
                            .shadow(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Students") },
                            onClick = {
                                selectedFilter = null
                                showFilterMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.People, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Present Only") },
                            onClick = {
                                selectedFilter = AttendanceFilter.PRESENT
                                showFilterMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AttendanceColors.Success
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Absent Only") },
                            onClick = {
                                selectedFilter = AttendanceFilter.ABSENT
                                showFilterMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = AttendanceColors.Error
                                )
                            }
                        )
                        if (selectedFilter != null) {
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Clear Filter", color = AttendanceColors.Error) },
                                onClick = {
                                    selectedFilter = null
                                    showFilterMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = null,
                                        tint = AttendanceColors.Error
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Showing ${filteredList.size} of ${studentList.size} students",
                        fontSize = 12.sp,
                        color = AttendanceColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Attendance Table
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AttendanceColors.Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {

                            // ✅ FIXED: Header Row — weight called inside RowScope
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                AttendanceColors.Primary,
                                                AttendanceColors.PrimaryLight
                                            )
                                        )
                                    )
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TableHeaderCell(
                                    text = "Roll No.",
                                    modifier = Modifier.weight(2f)
                                )
                                TableHeaderCell(
                                    text = "Student Name",
                                    modifier = Modifier.weight(3f)
                                )
                                TableHeaderCell(
                                    text = "Status",
                                    modifier = Modifier.weight(1.5f),
                                    textAlign = TextAlign.Center
                                )
                            }

                            if (filteredList.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.SearchOff,
                                            contentDescription = "No results",
                                            modifier = Modifier.size(64.dp),
                                            tint = AttendanceColors.TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "No students found",
                                            fontSize = 16.sp,
                                            color = AttendanceColors.TextSecondary
                                        )
                                        if (searchQuery.isNotBlank() || selectedFilter != null) {
                                            Text(
                                                text = "Try adjusting your search or filter",
                                                fontSize = 14.sp,
                                                color = AttendanceColors.TextSecondary
                                            )
                                        }
                                    }
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(filteredList) { student ->
                                        StudentAttendanceRow(student)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    count: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AttendanceColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = AttendanceColors.TextSecondary
                )
                Text(
                    text = count.toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Icon(
                icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = color.copy(alpha = 0.7f)
            )
        }
    }
}

// ✅ FIXED: Accepts Modifier from outside so weight is set in RowScope
@Composable
fun TableHeaderCell(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 8.dp),
        fontWeight = FontWeight.Bold,
        color = Color.White,
        fontSize = 14.sp,
        textAlign = textAlign
    )
}

// ✅ FIXED: Status badge no longer wraps text
@Composable
fun StudentAttendanceRow(student: StudentAttendance) {
    val isPresent = student.status == "present"
    val statusColor = if (isPresent) AttendanceColors.Success else AttendanceColors.Error
    val statusBgColor = if (isPresent) AttendanceColors.PresentBg else AttendanceColors.AbsentBg

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isPresent) AttendanceColors.PresentBg.copy(alpha = 0.3f)
                else AttendanceColors.AbsentBg.copy(alpha = 0.3f)
            )
            .padding(vertical = 10.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = student.roll.takeLast(3),
            modifier = Modifier
                .weight(2f)
                .padding(horizontal = 4.dp),
            fontSize = 13.sp,
            color = AttendanceColors.TextPrimary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = student.name,
            modifier = Modifier
                .weight(3f)
                .padding(horizontal = 4.dp),
            fontSize = 13.sp,
            color = AttendanceColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // ✅ FIXED: Outer Box holds the weight, inner Box is the pill badge
        Box(
            modifier = Modifier
                .weight(1.5f)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .background(statusBgColor, RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPresent) "PRESENT" else "ABSENT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }

    HorizontalDivider(
        color = AttendanceColors.Divider,
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

data class StudentAttendance(
    val studentId: String,
    val roll: String,
    val name: String,
    val status: String
)

enum class AttendanceFilter {
    PRESENT, ABSENT
}

suspend fun fetchAttendanceSummary(
    teacherId: String,
    generatedAt: String
): List<JSONObject> {
    return try {
        val json = JSONObject().apply {
            put("teacherId", teacherId)
            put("generatedAt", generatedAt)
        }

        val body = json.toString()
            .toRequestBody("application/json".toMediaTypeOrNull())

        val response = RetrofitClient.analysisInstance
            .getAttendanceSummary(body)

        if (response.isSuccessful) {
            val resString = response.body()?.string()
            val jsonObj = JSONObject(resString ?: "{}")
            val dataArray = jsonObj.getJSONArray("data")
            val list = mutableListOf<JSONObject>()

            for (i in 0 until dataArray.length()) {
                list.add(dataArray.getJSONObject(i))
            }
            list
        } else emptyList()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}