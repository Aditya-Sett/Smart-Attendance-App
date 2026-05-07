package com.mckv.attendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.mckv.attendance.data.local.SessionManager
import com.mckv.attendance.data.remote.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import androidx.activity.compose.BackHandler

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
    val EditedHighlight = Color(0xFFFFF9C4)
    val EditedBorder = Color(0xFFFBC02D)
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

    // ----- core state -----
    var studentList by remember { mutableStateOf(listOf<StudentAttendance>()) }
    var editableList by remember { mutableStateOf(listOf<StudentAttendance>()) }

    // originalStatusMap: stores the server-fetched status for each student.
    // A student's row is highlighted ONLY when current status ≠ original status.
    // Uses mutableStateMapOf so Compose tracks individual key reads and
    // triggers recomposition of any row whose entry changes.
    val originalStatusMap = remember { mutableStateMapOf<String, String>() }

    var filteredList by remember { mutableStateOf(listOf<StudentAttendance>()) }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<AttendanceFilter?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }

    var isEditMode by remember { mutableStateOf(false) }
    var hasChanges by remember { mutableStateOf(false) }

    val presentStudents = remember { mutableStateListOf<String>() }
    val absentStudents = remember { mutableStateListOf<String>() }

    var showSaveDialog by remember { mutableStateOf(false) }

    var reloadTrigger by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()

    // ----- load / reload from API -----
    LaunchedEffect(reloadTrigger) {
        isLoading = true
        val rawData = fetchAttendanceSummary(teacherId, generatedAt)

        // Deduplicate by studentId to prevent inconsistent counts caused
        // by the server returning duplicate rows across repeated requests.
        val seen = mutableSetOf<String>()
        val loaded = rawData.mapNotNull {
            val id = it.getString("studentId")
            if (seen.add(id)) {
                StudentAttendance(
                    studentId = id,
                    roll = it.getString("collegeRoll"),
                    name = it.getString("username"),
                    status = it.getString("status")
                )
            } else null
        }

        studentList = loaded
        editableList = loaded

        // Populate originalStatusMap so highlight comparison always uses
        // the server-side ground truth. Clear first to remove stale keys.
        originalStatusMap.clear()
        loaded.forEach { originalStatusMap[it.studentId] = it.status }

        isLoading = false
    }

    // ----- filter always runs against editableList (live) -----
    LaunchedEffect(searchQuery, selectedFilter, editableList) {
        delay(300)
        var result = editableList

        if (searchQuery.isNotBlank()) {
            result = result.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.roll.contains(searchQuery, ignoreCase = true) ||
                        it.studentId.contains(searchQuery, ignoreCase = true)
            }
        }

        when (selectedFilter) {
            AttendanceFilter.PRESENT -> result = result.filter { it.status == "present" }
            AttendanceFilter.ABSENT  -> result = result.filter { it.status == "absent"  }
            null -> {}
        }

        filteredList = result
    }

    // Counts always derived from editableList so summary is live during editing
    val presentCount = editableList.count { it.status == "present" }
    val absentCount  = editableList.count { it.status == "absent"  }
    val totalCount   = editableList.size
    val attendancePercentage = if (totalCount > 0) (presentCount * 100f / totalCount) else 0f

    // editedStudentIds: computed directly (NO remember cache) so it always
    // reflects the live editableList vs originalStatusMap comparison.
    // A student is "edited" only when their current status differs from the
    // server-fetched original — toggling back removes them from this set.
    val editedStudentIds: Set<String> = editableList
        .filter { originalStatusMap[it.studentId] != it.status }
        .map { it.studentId }
        .toSet()

    // Back-press in edit mode with unsaved changes → show save dialog
    BackHandler(enabled = isEditMode && hasChanges) {
        showSaveDialog = true
    }

    // ----- Full-screen saving overlay -----
    if (isSaving) {
        Dialog(
            onDismissRequest = { /* not dismissible while saving */ },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AttendanceColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = AttendanceColors.Primary,
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Saving changes...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = AttendanceColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Please wait",
                        fontSize = 13.sp,
                        color = AttendanceColors.TextSecondary
                    )
                }
            }
        }
    }

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
                            text = convertToIST(generatedAt),
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
                actions = {
                    if (isEditMode) {
                        IconButton(
                            onClick = { showSaveDialog = true },
                            enabled = hasChanges
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }

                    IconButton(onClick = {
                        if (isEditMode) {
                            // Cancel — restore to last saved state
                            editableList = studentList
                            presentStudents.clear()
                            absentStudents.clear()
                            hasChanges = false
                        }
                        isEditMode = !isEditMode
                    }) {
                        Icon(
                            if (isEditMode) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = if (isEditMode) "Cancel Edit" else "Edit"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AttendanceColors.Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
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
                    // Summary Cards
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

                    // Attendance Rate Card
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

                    // Search & Filter Row
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
                                Badge(containerColor = AttendanceColors.Primary) { Text("1") }
                            }
                        }) {
                            FilterChip(
                                selected = showFilterMenu,
                                onClick = { showFilterMenu = !showFilterMenu },
                                label = {
                                    Text(
                                        when (selectedFilter) {
                                            AttendanceFilter.PRESENT -> "Present"
                                            AttendanceFilter.ABSENT  -> "Absent"
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

                    // Filter Dropdown
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false },
                        modifier = Modifier.shadow(8.dp).clip(RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Students") },
                            onClick = { selectedFilter = null; showFilterMenu = false },
                            leadingIcon = { Icon(Icons.Default.People, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Present Only") },
                            onClick = { selectedFilter = AttendanceFilter.PRESENT; showFilterMenu = false },
                            leadingIcon = {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AttendanceColors.Success)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Absent Only") },
                            onClick = { selectedFilter = AttendanceFilter.ABSENT; showFilterMenu = false },
                            leadingIcon = {
                                Icon(Icons.Default.Cancel, contentDescription = null, tint = AttendanceColors.Error)
                            }
                        )
                        if (selectedFilter != null) {
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Clear Filter", color = AttendanceColors.Error) },
                                onClick = { selectedFilter = null; showFilterMenu = false },
                                leadingIcon = {
                                    Icon(Icons.Default.Clear, contentDescription = null, tint = AttendanceColors.Error)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Showing ${filteredList.size} of ${editableList.size} students",
                        fontSize = 12.sp,
                        color = AttendanceColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    if (isEditMode && editedStudentIds.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(AttendanceColors.EditedHighlight, RoundedCornerShape(2.dp))
                                    .border(1.dp, AttendanceColors.EditedBorder, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${editedStudentIds.size} record(s) modified — unsaved",
                                fontSize = 12.sp,
                                color = AttendanceColors.EditedBorder,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Attendance Table
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AttendanceColors.Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(AttendanceColors.Primary, AttendanceColors.PrimaryLight)
                                        )
                                    )
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TableHeaderCell(text = "Roll No.", modifier = Modifier.weight(2f))
                                TableHeaderCell(text = "Student Name", modifier = Modifier.weight(3f))
                                TableHeaderCell(
                                    text = "Status",
                                    modifier = Modifier.weight(2f),
                                    textAlign = TextAlign.Center
                                )
                            }

                            if (filteredList.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.SearchOff,
                                            contentDescription = "No results",
                                            modifier = Modifier.size(64.dp),
                                            tint = AttendanceColors.TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("No students found", fontSize = 16.sp, color = AttendanceColors.TextSecondary)
                                        if (searchQuery.isNotBlank() || selectedFilter != null) {
                                            Text(
                                                "Try adjusting your search or filter",
                                                fontSize = 14.sp,
                                                color = AttendanceColors.TextSecondary
                                            )
                                        }
                                    }
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(filteredList, key = { it.studentId }) { student ->
                                        // isEdited: true only when current status differs from the
                                        // original server-fetched status. Toggling back to original
                                        // makes this false and removes the yellow highlight.
                                        val isEdited = originalStatusMap[student.studentId] != student.status

                                        StudentAttendanceRow(
                                            student = student,
                                            isEditMode = isEditMode,
                                            isEdited = isEdited,
                                            onStatusToggle = { stu ->
                                                // Flip present ↔ absent
                                                val newStatus = if (stu.status == "present") "absent" else "present"

                                                editableList = editableList.map {
                                                    if (it.studentId == stu.studentId) it.copy(status = newStatus) else it
                                                }

                                                // Keep present/absent tracking lists in sync
                                                if (newStatus == "present") {
                                                    if (!presentStudents.contains(stu.studentId)) presentStudents.add(stu.studentId)
                                                    absentStudents.remove(stu.studentId)
                                                } else {
                                                    if (!absentStudents.contains(stu.studentId)) absentStudents.add(stu.studentId)
                                                    presentStudents.remove(stu.studentId)
                                                }

                                                // hasChanges: true only if at least one student
                                                // still differs from their original status
                                                hasChanges = editableList.any {
                                                    originalStatusMap[it.studentId] != it.status
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Save / Discard confirmation dialog
    // Triggered by: tapping the Save icon OR pressing the system back button
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Unsaved Changes") },
            text = { Text("Do you want to save the changes you made?") },
            confirmButton = {
                TextButton(onClick = {
                    showSaveDialog = false

                    val presentSnapshot = presentStudents.toList()
                    val absentSnapshot  = absentStudents.toList()

                    isEditMode = false
                    hasChanges = false
                    presentStudents.clear()
                    absentStudents.clear()

                    scope.launch {
                        isSaving = true

                        val success = saveAttendanceChanges(
                            teacherId,
                            generatedAt,
                            presentSnapshot,
                            absentSnapshot
                        )

                        isSaving = false

                        if (success) {
                            reloadTrigger++
                        } else {
                            // Restore on failure so user doesn't lose work
                            editableList = studentList
                        }
                    }
                }) {
                    Text("Save", color = AttendanceColors.Primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                // Discard: revert all changes, exit edit mode, then navigate back.
                // Previously this only reset state but never called navigateUp(),
                // so the user was stuck on the same screen after choosing Discard.
                TextButton(onClick = {
                    showSaveDialog = false
                    editableList = studentList
                    presentStudents.clear()
                    absentStudents.clear()
                    hasChanges = false
                    isEditMode = false
                    navController.navigateUp() // ← KEY FIX: actually go back
                }) {
                    Text("Discard", color = AttendanceColors.Error)
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Sub-composables
// ---------------------------------------------------------------------------

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
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = title, fontSize = 12.sp, color = AttendanceColors.TextSecondary)
                Text(text = count.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
            }
            Icon(icon, contentDescription = title, modifier = Modifier.size(32.dp), tint = color.copy(alpha = 0.7f))
        }
    }
}

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

@Composable
fun StudentAttendanceRow(
    student: StudentAttendance,
    isEditMode: Boolean,
    isEdited: Boolean,
    // Simple toggle callback — no three-dot menu, no explicit newStatus parameter.
    // In edit mode the status chip itself is the toggle button.
    onStatusToggle: (StudentAttendance) -> Unit
) {
    val isPresent = student.status == "present"
    val statusColor   = if (isPresent) AttendanceColors.Success else AttendanceColors.Error
    val statusBgColor = if (isPresent) AttendanceColors.PresentBg else AttendanceColors.AbsentBg

    // Row background: yellow highlight only when isEdited == true.
    // If the user toggles a student back to their original status,
    // isEdited becomes false and the yellow immediately disappears.
    val rowBg = when {
        isEdited  -> AttendanceColors.EditedHighlight
        isPresent -> AttendanceColors.PresentBg.copy(alpha = 0.3f)
        else      -> AttendanceColors.AbsentBg.copy(alpha = 0.3f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isEdited) Modifier.border(
                    width = 3.dp,
                    color = AttendanceColors.EditedBorder,
                    shape = RoundedCornerShape(0.dp)
                ) else Modifier
            )
            .background(rowBg)
            .padding(vertical = 10.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = student.roll.takeLast(3),
            modifier = Modifier.weight(2f).padding(horizontal = 4.dp),
            fontSize = 13.sp,
            color = AttendanceColors.TextPrimary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = student.name,
            modifier = Modifier.weight(3f).padding(horizontal = 4.dp),
            fontSize = 13.sp,
            color = AttendanceColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Status chip — acts as a toggle button in edit mode.
        // No three-dot menu. Tapping flips present ↔ absent instantly.
        Box(
            modifier = Modifier
                .weight(2f)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .background(statusBgColor, RoundedCornerShape(20.dp))
                    .then(
                        if (isEditMode) {
                            Modifier
                                .border(
                                    width = 1.5.dp,
                                    color = statusColor.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { onStatusToggle(student) }
                        } else Modifier
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                    // Small swap icon hints the chip is tappable in edit mode
                    if (isEditMode) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Toggle status",
                            modifier = Modifier.size(12.dp),
                            tint = statusColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }

    HorizontalDivider(
        color = AttendanceColors.Divider,
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

// ---------------------------------------------------------------------------
// Data classes / enums
// ---------------------------------------------------------------------------

data class StudentAttendance(
    val studentId: String,
    val roll: String,
    val name: String,
    val status: String
)

enum class AttendanceFilter { PRESENT, ABSENT }

// ---------------------------------------------------------------------------
// Network helpers
// ---------------------------------------------------------------------------

suspend fun fetchAttendanceSummary(
    teacherId: String,
    generatedAt: String
): List<JSONObject> {
    return try {
        val json = JSONObject().apply {
            put("teacherId", teacherId)
            put("generatedAt", generatedAt)
        }
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val response = RetrofitClient.analysisInstance.getAttendanceSummary(body)

        if (response.isSuccessful) {
            val jsonObj = JSONObject(response.body()?.string() ?: "{}")
            val dataArray = jsonObj.getJSONArray("data")
            (0 until dataArray.length()).map { dataArray.getJSONObject(it) }
        } else emptyList()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

suspend fun saveAttendanceChanges(
    teacherId: String,
    generatedAt: String,
    presentList: List<String>,
    absentList: List<String>
): Boolean {
    return try {
        val json = JSONObject().apply {
            put("teacherId", teacherId)
            put("generatedAt", generatedAt)
            put("present_student", JSONArray(presentList))
            put("absent_student", JSONArray(absentList))
        }
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val response = RetrofitClient.instance.saveAttendance2(body)

        if (response.isSuccessful) {
            println("Saved successfully")
            true
        } else {
            println("Save failed: ${response.code()} — ${response.errorBody()?.string()}")
            false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}