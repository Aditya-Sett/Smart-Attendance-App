package com.mckv.attendance.ui.screens

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mckv.attendance.data.local.SessionManager
import com.mckv.attendance.data.remote.RetrofitClient
import com.mckv.attendance.data.remote.RetrofitClient.BASE_ANALYSIS_URL
import androidx.core.net.toUri
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────
// Data Models
// ─────────────────────────────────────────────

data class AttendanceEntry(
    val datetime: String,
    val status: String
)

data class StudentReport(
    val studentId: String,
    val name: String,
    val roll: String,
    val percentage: Double,
    val sessions: List<AttendanceEntry>
)

// ─────────────────────────────────────────────
// Repository
// ─────────────────────────────────────────────

class AttendanceRepository {

    suspend fun fetchReport(requestJson: JSONObject): List<StudentReport> {
        val body = requestJson.toString()
            .toRequestBody("application/json".toMediaTypeOrNull())
        val response = RetrofitClient.analysisInstance.getReport(body)
        if (!response.isSuccessful) throw Exception("API Error")
        val json = JSONObject(response.body()!!.string())
        val dataArray = json.getJSONArray("data")
        return (0 until dataArray.length()).map { parseStudent(dataArray.getJSONObject(it)) }
    }

    private fun parseStudent(json: JSONObject): StudentReport {
        val sessions = mutableListOf<AttendanceEntry>()
        json.keys().forEach { key ->
            if (key.contains("-")) {
                sessions.add(AttendanceEntry(datetime = key, status = json.getString(key)))
            }
        }
        return StudentReport(
            studentId = json.getString("studentId"),
            name = json.getString("username"),
            roll = json.getString("collegeRoll"),
            percentage = json.getDouble("percentage"),
            sessions = sessions.sortedBy { it.datetime }
        )
    }
}

// ─────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────

class ReportGenetateViewModel : ViewModel() {
    private val repo = AttendanceRepository()
    var students by mutableStateOf<List<StudentReport>>(emptyList())
    var loading by mutableStateOf(false)
    var showTable by mutableStateOf(false)

    fun generateReport(request: JSONObject) {
        viewModelScope.launch {
            loading = true
            showTable = false
            try {
                students = repo.fetchReport(request)
                delay(300)
                showTable = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
            loading = false
        }
    }
}

// ─────────────────────────────────────────────
// Helper – recalculate percentage for a date range
// ─────────────────────────────────────────────

/**
 * Parses "dd-MM-yy HH:mm" or similar stored datetime strings.
 * Adjust the pattern to match whatever your server returns.
 */
@RequiresApi(Build.VERSION_CODES.O)
private val SESSION_FMT = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm")

@RequiresApi(Build.VERSION_CODES.O)
private fun parseSessionDate(raw: String): LocalDateTime? = runCatching {
    LocalDateTime.parse(raw.trim(), SESSION_FMT)
}.getOrNull()

/**
 * Filters a student's sessions to the given date window and
 * recomputes the attendance percentage over that window.
 */
@RequiresApi(Build.VERSION_CODES.O)
private fun applyDateFilter(
    student: StudentReport,
    from: LocalDateTime?,
    to: LocalDateTime?
): StudentReport {
    if (from == null && to == null) return student
    val filtered = student.sessions.filter { entry ->
        val dt = parseSessionDate(entry.datetime) ?: return@filter true
        (from == null || !dt.isBefore(from)) && (to == null || !dt.isAfter(to))
    }
    val total = filtered.size
    val present = filtered.count { it.status.equals("P", ignoreCase = true) }
    val newPct = if (total == 0) 0.0 else (present.toDouble() / total) * 100.0
    return student.copy(sessions = filtered, percentage = newPct)
}

// ─────────────────────────────────────────────
// Main Screen
// ─────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportGenerateScreen(viewModel: ReportGenetateViewModel) {

    /* ── form state ── */
    var selectedTab by remember { mutableStateOf(0) }
    var department by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var sem by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var otherTeacherId by remember { mutableStateOf("") }

    /* ── filter panel visibility ── */
    var showFilterPanel by remember { mutableStateOf(false) }

    /* ── search ── */
    var searchQuery by remember { mutableStateOf("") }

    /* ── date range (stored as strings "dd-MM-yy HH:mm" or empty) ── */
    var fromDateStr by remember { mutableStateOf("") }
    var toDateStr by remember { mutableStateOf("") }

    /* ── attendance % filter ── */
    var pctOperator by remember { mutableStateOf(">=") }   // ">=" or "<="
    var pctThresholdStr by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current

    val teacherId = if (selectedTab == 0) SessionManager.userDetails?.userId ?: ""
    else otherTeacherId

    /* ── derived: apply all active filters ── */
    val fromDt = remember(fromDateStr) { if (fromDateStr.isBlank()) null else parseSessionDate(fromDateStr) }
    val toDt   = remember(toDateStr)   { if (toDateStr.isBlank())   null else parseSessionDate(toDateStr)   }
    val pctThreshold = pctThresholdStr.toDoubleOrNull()

    val filteredStudents = remember(
        viewModel.students, searchQuery, fromDt, toDt, pctOperator, pctThreshold
    ) {
        viewModel.students
            // 1. date-range filter + percentage recalculation
            .map { applyDateFilter(it, fromDt, toDt) }
            // 2. percentage threshold filter
            .filter { student ->
                if (pctThreshold == null) true
                else when (pctOperator) {
                    ">=" -> student.percentage >= pctThreshold
                    "<=" -> student.percentage <= pctThreshold
                    else -> true
                }
            }
            // 3. search by name or roll
            .filter { student ->
                if (searchQuery.isBlank()) true
                else student.name.contains(searchQuery, ignoreCase = true) ||
                        student.roll.contains(searchQuery, ignoreCase = true)
            }
    }

    val gradientBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF667eea), Color(0xFF764ba2)),
        start = Offset(0f, 0f), end = Offset(1000f, 0f)
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp,
                shape = RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gradientBrush)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 20.dp)
                ) {
                    Text("Attendance Report", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Generate & Export Reports", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F7FA))
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {

                // ── Tabs ──────────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.White,
                        contentColor = Color(0xFF667eea),
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Self Report", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.People, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Others Report", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Filters Card ──────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {

                        Text("Filters", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                        Spacer(Modifier.height(4.dp))
                        Text("Select criteria to generate report", fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.height(16.dp))

                        AnimatedVisibility(visible = selectedTab == 1, enter = fadeIn() + slideInVertically(), exit = fadeOut() + slideOutVertically()) {
                            Column {
                                OutlinedTextField(
                                    value = otherTeacherId,
                                    onValueChange = { otherTeacherId = it },
                                    label = { Text("Teacher ID *", color = Color(0xFF667eea)) },
                                    placeholder = { Text("Enter Teacher ID") },
                                    leadingIcon = { Icon(Icons.Default.Badge, null, tint = Color(0xFF667eea)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = Color(0xFF667eea),
                                        unfocusedBorderColor = Color(0xFFE0E0E0)
                                    )
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }

                        AutoCompleteField("Department *", listOf("CSE", "CSEDS", "CSEAIML", "IT", "EE", "ECE", "ME", "AUE"), department, { department = it }, Icons.Default.Business)
                        Spacer(Modifier.height(12.dp))
                        AutoCompleteField("Academic Year *", listOf("2025-26", "2024-25", "2023-24", "2022-23"), year, { year = it }, Icons.Default.CalendarToday)
                        Spacer(Modifier.height(12.dp))
                        AutoCompleteField("Semester *", listOf("1st","2nd","3rd","4th","5th", "6th","7th", "8th"), sem, { sem = it }, Icons.Default.Grade)
                        Spacer(Modifier.height(12.dp))
                        AutoCompleteField("Subject *", listOf("HM-HU101", "HM-HU501", "HM-HU604", "HM-HU702", "HM-HU291", "HM-HU591", "BS-PH101", "BS-PH201", "BS-M101", "BS-CH201", "BS-CH101","BS-M201","BS-M303","BS-M301","BS-BIO301","BS-M404","PC-CS402","PC-CS403","PC-CS404","PC-CS(D)401","PC-CS492","PC-CS(D)491","PC-CS493","BS-M494","BS-PH191", "BS-PH291","BS-CH291","BS-CH191","ES-EE101","ES-CS201","ES-AUE301","ES-AUE302","ES-AUE401","ES-EE191","ES-ME191","ES-CS291","ES-ME292","PC-AUE301","PC-AUE302","PC-AUE401","PC-AUE402","PC-AUE403","PC-AUE404","PC-AUE501","PC-AUE502","PC-AUE503","PC-AUE504","PC-AUE601","PC-AUE602","PC-AUE701","PC-AUE391","PC-AUE491","PC-AUE591","PC-AUE592","PC-AUE691","PC-AUE692","PC-AUE693","PC-AUE791","PC-AUE881","PE-AUE601","PE-AUE601A","PE-AUE601B","PE-AUE601C","PE-AUE701","PE-AUE701A","PE-AUE701B","PE-AUE701C","PE-AUE702","PE-AUE702A","PE-AUE702B","PE-AUE702C","PE-AUE801","PE-AUE801A","PE-AUE801B","PE-AUE801C","PE-AUE801D","PE-AUE802","PE-AUE802A","PE-AUE802B","PE-AUE802C","OE-AUE701","OE-AUE701A","OE-AUE701B","OE-AUE701C","OE-AUE701D","OE-AUE801","OE-AUE801A","OE-AUE801B","OE-AUE801C","OE-AUE802","OE-AUE802A","OE-AUE802B","OE-AUE802C","OE-AUE802D","PW-AUE581","PW-AUE681","PW-AUE781","PW-AUE882","MC471","MC571","MC671","MC673","MC772"), subject, { subject = it }, Icons.Default.MenuBook)
                        Spacer(Modifier.height(24.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    val request = JSONObject().apply {
                                        put("teacherId", teacherId)
                                        put("department", department)
                                        put("academicYear", year)
                                        put("sem", sem)
                                        put("subject", subject)
                                    }
                                    if (validateFields(selectedTab, teacherId, department, year, sem, subject)) {
                                        // Reset table filters when fetching fresh data
                                        searchQuery = ""
                                        fromDateStr = ""
                                        toDateStr = ""
                                        pctThresholdStr = ""
                                        showFilterPanel = false
                                        viewModel.generateReport(request)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667eea)),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Generate Report", fontWeight = FontWeight.SemiBold)
                            }

                            ExportMenu(teacherId, department, year, sem, subject)
                        }

                        Spacer(Modifier.height(8.dp))
                        Text("* Required fields", fontSize = 10.sp, color = Color.Gray)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Loading ───────────────────────────────────────────
                if (viewModel.loading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF667eea), strokeWidth = 2.dp)
                                Spacer(Modifier.width(12.dp))
                                Text("Generating Report...", color = Color(0xFF667eea), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // ── Table Filters + Search (shown only when table is visible) ──
                AnimatedVisibility(
                    visible = viewModel.showTable && viewModel.students.isNotEmpty(),
                    enter = fadeIn() + slideInVertically()
                ) {
                    Column {
                        Spacer(Modifier.height(12.dp))

                        // Search bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by Name or Roll No.") },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF667eea)) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF667eea),
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                containerColor = Color.White
                            )
                        )

                        Spacer(Modifier.height(8.dp))

                        // Filter toggle button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Active filter chips summary
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (fromDateStr.isNotBlank() || toDateStr.isNotBlank()) {
                                    FilterChip(
                                        selected = true,
                                        onClick = { fromDateStr = ""; toDateStr = "" },
                                        label = { Text("Date ✕", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFE8EAFF))
                                    )
                                }
                                if (pctThresholdStr.isNotBlank()) {
                                    FilterChip(
                                        selected = true,
                                        onClick = { pctThresholdStr = "" },
                                        label = { Text("% ✕", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFE8EAFF))
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { showFilterPanel = !showFilterPanel },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF667eea))
                            ) {
                                Icon(
                                    if (showFilterPanel) Icons.Default.FilterListOff else Icons.Default.FilterList,
                                    null, modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (showFilterPanel) "Hide Filters" else "Table Filters", fontSize = 13.sp)
                            }
                        }

                        // ── Expandable filter panel ───────────────────
                        AnimatedVisibility(visible = showFilterPanel, enter = fadeIn() + slideInVertically(), exit = fadeOut() + slideOutVertically()) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {

                                    // ── Date Range ──
                                    Text("Custom Date Range", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                                    Spacer(Modifier.height(4.dp))
                                    Text("Format: dd-MM-yy HH:mm  (e.g. 20-04-26 07:31)", fontSize = 11.sp, color = Color.Gray)
                                    Spacer(Modifier.height(10.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = fromDateStr,
                                            onValueChange = { fromDateStr = it },
                                            label = { Text("From", color = Color(0xFF667eea)) },
                                            placeholder = { Text("20-04-26 07:31", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                                focusedBorderColor = Color(0xFF667eea),
                                                unfocusedBorderColor = Color(0xFFE0E0E0)
                                            )
                                        )
                                        OutlinedTextField(
                                            value = toDateStr,
                                            onValueChange = { toDateStr = it },
                                            label = { Text("To", color = Color(0xFF667eea)) },
                                            placeholder = { Text("28-04-26 16:34", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                                focusedBorderColor = Color(0xFF667eea),
                                                unfocusedBorderColor = Color(0xFFE0E0E0)
                                            )
                                        )
                                    }

                                    Spacer(Modifier.height(16.dp))
                                    Divider(color = Color(0xFFF0F0F0))
                                    Spacer(Modifier.height(16.dp))

                                    // ── Attendance % Filter ──
                                    Text("Attendance % Filter", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                                    Spacer(Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Operator selector
                                        Card(
                                            modifier = Modifier.wrapContentWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0FF)),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                        ) {
                                            Row(modifier = Modifier.padding(4.dp)) {
                                                listOf(">=", "<=").forEach { op ->
                                                    Surface(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable { pctOperator = op }
                                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                                        color = if (pctOperator == op) Color(0xFF667eea) else Color.Transparent
                                                    ) {
                                                        Text(
                                                            op,
                                                            color = if (pctOperator == op) Color.White else Color(0xFF667eea),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 15.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = pctThresholdStr,
                                            onValueChange = { pctThresholdStr = it.filter { c -> c.isDigit() || c == '.' } },
                                            label = { Text("Attendance %", color = Color(0xFF667eea)) },
                                            placeholder = { Text("e.g. 75") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                                focusedBorderColor = Color(0xFF667eea),
                                                unfocusedBorderColor = Color(0xFFE0E0E0)
                                            )
                                        )
                                    }

                                    Spacer(Modifier.height(16.dp))

                                    // Reset button
                                    OutlinedButton(
                                        onClick = {
                                            fromDateStr = ""
                                            toDateStr = ""
                                            pctThresholdStr = ""
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336)),
                                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                                    ) {
                                        Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Reset Filters")
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Result count
                        Text(
                            text = "Showing ${filteredStudents.size} of ${viewModel.students.size} students",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // ── Attendance Table ───────────────────────────────────
                AnimatedVisibility(
                    visible = viewModel.showTable && viewModel.students.isNotEmpty(),
                    enter = slideInVertically { it } + fadeIn()
                ) {
                    if (filteredStudents.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                                    Spacer(Modifier.height(12.dp))
                                    Text("No students match the filters", color = Color.Gray, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp, max = 800.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            AttendanceTable(filteredStudents)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Validation
// ─────────────────────────────────────────────

private fun validateFields(selectedTab: Int, teacherId: String, department: String, year: String, sem: String, subject: String): Boolean {
    return when (selectedTab) {
        0 -> department.isNotBlank() && year.isNotBlank() && sem.isNotBlank() && subject.isNotBlank()
        1 -> teacherId.isNotBlank() && department.isNotBlank() && year.isNotBlank() && sem.isNotBlank() && subject.isNotBlank()
        else -> false
    }
}

// ─────────────────────────────────────────────
// AutoCompleteField
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCompleteField(
    label: String,
    options: List<String>,
    value: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it); expanded = true },
            label = { Text(label, color = Color(0xFF667eea)) },
            leadingIcon = icon?.let { { Icon(it, null, tint = Color(0xFF667eea)) } },
            modifier = modifier.menuAnchor().fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF667eea),
                unfocusedBorderColor = Color(0xFFE0E0E0)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White).heightIn(max = 300.dp)
        ) {
            val filtered = options.filter { it.contains(value, ignoreCase = true) }
            if (filtered.isEmpty()) {
                DropdownMenuItem(text = { Text("No options found", color = Color.Gray) }, onClick = { expanded = false }, enabled = false)
            } else {
                filtered.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, fontWeight = if (option == value) FontWeight.Bold else FontWeight.Normal) },
                        onClick = { onValueChange(option); expanded = false },
                        modifier = Modifier.background(if (option == value) Color(0xFFF0F0FF) else Color.White)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// AttendanceTable
// ─────────────────────────────────────────────

@Composable
fun AttendanceTable(data: List<StudentReport>) {
    val allDates = data.flatMap { it.sessions.map { s -> s.datetime } }.distinct().sorted()
    val horizontalScroll = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().horizontalScroll(horizontalScroll)) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF667eea),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp)) {
                Text("Roll No", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                Text("Student Name", modifier = Modifier.width(140.dp), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                allDates.forEach { date ->
                    Text(date, modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
                Text("Attendance %", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }

        // Rows
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(data) { student ->
                val statusColor = when {
                    student.percentage >= 85 -> Color(0xFF4CAF50)
                    student.percentage >= 75 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }

                Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 1.dp) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(student.roll, modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                        Text(student.name, modifier = Modifier.width(140.dp), fontSize = 12.sp, color = Color(0xFF555555))

                        allDates.forEach { date ->
                            val status = student.sessions.find { it.datetime == date }?.status ?: "-"
                            Box(modifier = Modifier.width(100.dp).padding(2.dp), contentAlignment = Alignment.Center) {
                                Surface(
                                    modifier = Modifier.width(40.dp).height(28.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (status.uppercase()) {
                                        "P" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                        "A" -> Color(0xFFF44336).copy(alpha = 0.2f)
                                        "L" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                        else -> Color.Gray.copy(alpha = 0.1f)
                                    }
                                ) {
                                    Text(
                                        status,
                                        modifier = Modifier.fillMaxSize(),
                                        textAlign = TextAlign.Center,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (status.uppercase()) {
                                            "P" -> Color(0xFF4CAF50)
                                            "A" -> Color(0xFFF44336)
                                            "L" -> Color(0xFFFF9800)
                                            else -> Color.Gray
                                        },
                                        lineHeight = 28.sp
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.width(100.dp).padding(4.dp),
                            shape = CircleShape,
                            color = statusColor.copy(alpha = 0.1f)
                        ) {
                            Text(
                                String.format("%.1f%%", student.percentage),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }
                }
                Divider(color = Color(0xFFF0F0F0), thickness = 1.dp)
            }
        }
    }
}

// ─────────────────────────────────────────────
// ExportMenu
// ─────────────────────────────────────────────

@Composable
fun ExportMenu(teacherId: String, department: String, year: String, sem: String, subject: String) {
    val analysisUrl = BASE_ANALYSIS_URL
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF667eea)),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Export", fontWeight = FontWeight.SemiBold)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White).clip(RoundedCornerShape(12.dp))
        ) {
            listOf("excel" to Pair(Icons.Default.GridOn, Color(0xFF4CAF50)), "pdf" to Pair(Icons.Default.PictureAsPdf, Color(0xFFF44336))).forEach { (type, meta) ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(meta.first, null, modifier = Modifier.size(20.dp), tint = meta.second)
                            Spacer(Modifier.width(12.dp))
                            Text("Export as ${type.replaceFirstChar { it.uppercase() }}", fontWeight = FontWeight.Medium)
                        }
                    },
                    onClick = {
                        val url = "${analysisUrl}api/attendance/report?teacherId=$teacherId&department=$department&academicYear=$year&sem=$sem&subject=$subject&export=$type"
                        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                        expanded = false
                    }
                )
            }
        }
    }
}