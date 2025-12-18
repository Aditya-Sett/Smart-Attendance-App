package com.mckv.attendance.ui.screens

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.room.util.copy
import com.mckv.attendance.data.remote.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadCurriculumScreen(navController: NavHostController) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Form state
    var selectedClass by remember { mutableStateOf("") }
    var selectedDepartment by remember { mutableStateOf("") }
    var totalSemesters by remember { mutableIntStateOf(0) }
    var effectiveYear by remember { mutableStateOf("") }

    // Semester data
    val semestersData = remember { mutableStateMapOf<Int, List<CourseDetail>>() }

    // Calculate total credits
    val totalCredits by remember(semestersData) {
        derivedStateOf {
            semestersData.values.flatten().sumOf { it.credits.toIntOrNull() ?: 0 }
        }
    }

    // Form validation
    val isFormValid by remember(
        selectedClass,
        selectedDepartment,
        totalSemesters,
        effectiveYear,
        semestersData
    ) {
        derivedStateOf {
            selectedClass.isNotEmpty() &&
                    selectedDepartment.isNotEmpty() &&
                    totalSemesters > 0 &&
                    effectiveYear.isNotEmpty() &&
                    semestersData.size == totalSemesters &&
                    semestersData.values.all { it.isNotEmpty() && it.all { course ->
                        course.name.isNotEmpty() && course.code.isNotEmpty() && course.credits.isNotEmpty()
                    } }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Curriculum") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            // Handle upload logic
                            val success = uploadCurriculumToBackend(
                                selectedClass,
                                selectedDepartment,
                                totalSemesters,
                                effectiveYear,
                                semestersData,
                                totalCredits
                            )

                            if (success) {
                                snackbarHostState.showSnackbar("Curriculum uploaded successfully!")
                                navController.popBackStack()
                            } else {
                                snackbarHostState.showSnackbar("Failed to upload curriculum")
                            }
                        }
                    },
                    enabled = isFormValid,
                    modifier = Modifier
                        .height(56.dp) // Same as ExtendedFloatingActionButton
                        .defaultMinSize(minWidth = 80.dp) // Minimum width
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = "Upload",
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Upload")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Basic Information Section
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Basic Information",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        // Class Dropdown
                        var expandedClass by remember { mutableStateOf(false) }
                        val classes = listOf("BTECH", "BBA", "MBA")

                        ExposedDropdownMenuBox(
                            expanded = expandedClass,
                            onExpandedChange = { expandedClass = !expandedClass }
                        ) {
                            OutlinedTextField(
                                value = selectedClass,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Class *") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedClass)
                                },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = expandedClass,
                                onDismissRequest = { expandedClass = false }
                            ) {
                                classes.forEach { className ->
                                    DropdownMenuItem(
                                        text = { Text(className) },
                                        onClick = {
                                            selectedClass = className
                                            expandedClass = false
                                        }
                                    )
                                }
                            }
                        }

                        // Department Dropdown
                        var expandedDept by remember { mutableStateOf(false) }
                        val departments = listOf(
                            "CSE",
                            "CSEDS",
                            "CSEAIML",
                            "IT",
                            "EE",
                            "ECE",
                            "ME",
                            "AUE",
                            "BBA",
                            "MBA"
                        )

                        ExposedDropdownMenuBox(
                            expanded = expandedDept,
                            onExpandedChange = { expandedDept = !expandedDept }
                        ) {
                            OutlinedTextField(
                                value = selectedDepartment,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Department *") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDept)
                                },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = expandedDept,
                                onDismissRequest = { expandedDept = false }
                            ) {
                                departments.forEach { dept ->
                                    DropdownMenuItem(
                                        text = { Text(dept) },
                                        onClick = {
                                            selectedDepartment = dept
                                            expandedDept = false
                                        }
                                    )
                                }
                            }
                        }

                        // Total Semesters
                        OutlinedTextField(
                            value = if (totalSemesters == 0) "" else totalSemesters.toString(),
                            onValueChange = { value ->
                                totalSemesters = value.toIntOrNull() ?: 0
                                // Initialize semester data structure
                                if (totalSemesters > 0) {
                                    (1..totalSemesters).forEach { semester ->
                                        if (!semestersData.containsKey(semester)) {
                                            semestersData[semester] = listOf(CourseDetail())
                                        }
                                    }
                                }
                            },
                            label = { Text("Total Semesters *") },
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Effective from Academic Year
                        OutlinedTextField(
                            value = effectiveYear,
                            onValueChange = { effectiveYear = it },
                            label = { Text("Effective from Academic Year *") },
                            placeholder = { Text("e.g., 2024-25") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Save & Next Button
                        Button(
                            onClick = {
                                if (selectedClass.isNotEmpty() &&
                                    selectedDepartment.isNotEmpty() &&
                                    totalSemesters > 0 &&
                                    effectiveYear.isNotEmpty()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Basic information saved!")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedClass.isNotEmpty() &&
                                    selectedDepartment.isNotEmpty() &&
                                    totalSemesters > 0 &&
                                    effectiveYear.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save & Next")
                        }
                    }
                }
            }

            // Semester Sections (only shown when totalSemesters > 0)
            if (totalSemesters > 0) {
                items(totalSemesters) { index ->
                    val semesterNumber = index + 1
                    SemesterSection(
                        semesterNumber = semesterNumber,
                        courses = semestersData[semesterNumber] ?: emptyList(),
                        onCoursesUpdate = { newCourses ->
                            semestersData[semesterNumber] = newCourses
                        }
                    )
                }

                item {
                    // Total Credits Summary
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Credits",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = totalCredits.toString(),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                item {
                    // Preview Button
                    Button(
                        onClick = {
                            navController.navigate("previewCurriculum/${selectedClass}/${selectedDepartment}/${totalSemesters}/${effectiveYear}/${totalCredits}")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isFormValid
                    ) {
                        Icon(Icons.Default.Preview, contentDescription = "Preview")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Preview")
                    }
                }
            }
        }
    }
}

@Composable
fun SemesterSection(
    semesterNumber: Int,
    courses: List<CourseDetail>,
    onCoursesUpdate: (List<CourseDetail>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Semester $semesterNumber",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                // Add Course Button
                IconButton(
                    onClick = {
                        val newCourses = courses.toMutableList()
                        newCourses.add(CourseDetail())
                        onCoursesUpdate(newCourses)
                    }
                ) {
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = "Add Course",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Course details
            courses.forEachIndexed { index, course ->
                CourseDetailRow(
                    courseDetail = course,
                    onUpdate = { updatedCourse ->
                        val newCourses = courses.toMutableList()
                        newCourses[index] = updatedCourse
                        onCoursesUpdate(newCourses)
                    },
                    onRemove = {
                        if (courses.size > 1) {
                            val newCourses = courses.toMutableList()
                            newCourses.removeAt(index)
                            onCoursesUpdate(newCourses)
                        }
                    },
                    canRemove = courses.size > 1
                )
            }
        }
    }
}

@Composable
fun CourseDetailRow(
    courseDetail: CourseDetail,
    onUpdate: (CourseDetail) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Course Name
            OutlinedTextField(
                value = courseDetail.name,
                onValueChange = { onUpdate(courseDetail.copy(name = it)) },
                label = { Text("Course Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Course Code
                OutlinedTextField(
                    value = courseDetail.code,
                    onValueChange = { onUpdate(courseDetail.copy(code = it)) },
                    label = { Text("Course Code *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                // Credits
                OutlinedTextField(
                    value = courseDetail.credits,
                    onValueChange = {
                        if (it.toFloatOrNull() != null || it.isEmpty()) {
                            onUpdate(courseDetail.copy(credits = it))
                        }
                    },
                    label = { Text("Credits *") },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.weight(0.5f),
                    singleLine = true
                )
            }

            // Remove button (if there's more than one course)
            if (canRemove) {
                OutlinedButton(
                    onClick = onRemove,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove Course",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove")
                }
            }
        }
    }
}

// Data class for course details
data class CourseDetail(
    val name: String = "",
    val code: String = "",
    val credits: String = ""
)

// Simulated upload function
suspend fun uploadCurriculumToBackend(
    className: String,
    department: String,
    totalSemesters: Int,
    effectiveYear: String,
    semesterData: Map<Int, List<CourseDetail>>,
    totalCredits: Int
): Boolean {
    return try {
    // Create curriculum object
    val curriculum = UploadedCurriculum(
        className = className,
        department = department,
        totalSemesters = totalSemesters,
        effectiveYear = effectiveYear,
        semesterData = semesterData,
        totalCredits = totalCredits,
        uploadedAt = System.currentTimeMillis()
    )

    // TODO: Implement actual backend upload logic
    // This is a simulation - replace with your API call
        val response = RetrofitClient.instance.uploadCurriculum(curriculum)
        response.isSuccessful

        // Simulate API call
        //kotlinx.coroutines.delay(2000)
        //println("Uploading curriculum: $curriculum")
        //true
    } catch (e: Exception) {
        false
    }
}

// Data class for uploaded curriculum
data class UploadedCurriculum(
    val className: String,
    val department: String,
    val totalSemesters: Int,
    val effectiveYear: String,
    val semesterData: Map<Int, List<CourseDetail>>,
    val totalCredits: Int,
    val uploadedAt: Long
)