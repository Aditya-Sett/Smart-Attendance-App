package com.mckv.attendance.ui.screens

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mckv.attendance.data.remote.RetrofitClient
import kotlinx.coroutines.launch
import org.json.JSONObject
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCurriculumScreen(
    navController: NavHostController,
    curriculumId: String? = null
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Form state
    var selectedClass by remember { mutableStateOf("") }
    var selectedDepartment by remember { mutableStateOf("") }
    var totalSemesters by remember { mutableIntStateOf(0) }
    var effectiveYear by remember { mutableStateOf("") }

    // Original data for comparison
    var originalData by remember { mutableStateOf<CurriculumData?>(null) }

    // Semester data
    val semestersData = remember { mutableStateMapOf<Int, List<CourseDetail>>() }

    // Dialog states
    var showSaveDialog by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var saveAsNew by remember { mutableStateOf(false) }

    // Calculate total credits
    val totalCredits by remember(semestersData) {
        derivedStateOf {
            semestersData.values.flatten().sumOf { it.credits.toDoubleOrNull() ?: 0.0 }
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

    // Load curriculum data
    LaunchedEffect(curriculumId) {
        curriculumId?.let { id ->
            try {
                val response = RetrofitClient.instance.getCurriculumById(id)
                if (response.isSuccessful) {
                    val bodyString = response.body()?.string() ?: ""
                    val json = JSONObject(bodyString)

                    if (json.getBoolean("success")) {
                        val data = json.getJSONObject("curriculum")
                        selectedClass = data.getString("className")
                        selectedDepartment = data.getString("department")
                        totalSemesters = data.getInt("totalSemesters")
                        effectiveYear = data.getString("effectiveYear")

                        // Parse semester data
                        val semesterObj = data.getJSONObject("semesterData")
                        val keys = semesterObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val semesterNum = key.toInt()

                            val coursesArray = semesterObj.getJSONArray(key)
                            val courses = mutableListOf<CourseDetail>()

                            for (j in 0 until coursesArray.length()) {
                                val courseObj = coursesArray.getJSONObject(j)

                                courses.add(
                                    CourseDetail(
                                        name = courseObj.getString("name"),
                                        code = courseObj.getString("code"),
                                        credits = courseObj.getDouble("credits").toString() // credits is int in backend
                                    )
                                )
                            }

                            semestersData[semesterNum] = courses
                        }

                        // Save original data for comparison
                        originalData = CurriculumData(
                            className = selectedClass,
                            department = selectedDepartment,
                            totalSemesters = totalSemesters,
                            effectiveYear = effectiveYear,
                            semestersData = semestersData.toMap()
                        )
                    }
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to load curriculum data")
            }
        }
    }

    // Check if data has changed
    val hasChanges by remember(
        selectedClass,
        selectedDepartment,
        totalSemesters,
        effectiveYear,
        semestersData
    ) {
        derivedStateOf {
            originalData?.let { original ->
                selectedClass != original.className ||
                        selectedDepartment != original.department ||
                        totalSemesters != original.totalSemesters ||
                        effectiveYear != original.effectiveYear ||
                        !semestersDataEquals(semestersData, original.semestersData)
            } ?: true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Curriculum") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasChanges) {
                            showSaveDialog = true
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Save button
                    IconButton(
                        onClick = {
                            if (isFormValid && hasChanges) {
                                showConfirmationDialog = true
                            }
                        },
                        enabled = isFormValid && hasChanges
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
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
        /*floatingActionButton = {
            if (isFormValid) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Button(
                        onClick = { showConfirmationDialog = true },
                        enabled = hasChanges,
                        modifier = Modifier
                            .height(56.dp)
                            .defaultMinSize(minWidth = 80.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = "Save",
                                modifier = Modifier.size(20.dp)
                            )
                            Text("Save Changes")
                        }
                    }
                }
            }
        }*/
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
                                val newValue = value.toIntOrNull() ?: 0
                                totalSemesters = newValue
                                // Initialize semester data structure
                                if (newValue > 0) {
                                    (1..newValue).forEach { semester ->
                                        if (!semestersData.containsKey(semester)) {
                                            semestersData[semester] = listOf(CourseDetail())
                                        }
                                    }
                                    // Remove extra semesters if reduced
                                    if (newValue < semestersData.size) {
                                        semestersData.keys.filter { it > newValue }.forEach {
                                            semestersData.remove(it)
                                        }
                                    }
                                } else {
                                    semestersData.clear()
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
            }
        }

        // Unsaved Changes Dialog
        if (showSaveDialog) {
            UnsavedChangesDialog(
                onSave = {
                    showSaveDialog = false
                    showConfirmationDialog = true
                },
                onDiscard = {
                    showSaveDialog = false
                    navController.popBackStack()
                },
                onCancel = {
                    showSaveDialog = false
                }
            )
        }

        // Save Confirmation Dialog
        if (showConfirmationDialog) {
            SaveConfirmationDialog(
                saveAsNew = saveAsNew,
                onSaveAsNewChanged = { saveAsNew = it },
                onConfirm = {
                    showConfirmationDialog = false
                    scope.launch {
                        val success = if (saveAsNew) {
                            // Save as new curriculum
                            saveCurriculumAsNew(
                                selectedClass,
                                selectedDepartment,
                                totalSemesters,
                                effectiveYear,
                                semestersData.toMap(), //semestersData
                                totalCredits
                            )
                        } else {
                            // Overwrite existing curriculum
                            updateCurriculum(
                                curriculumId!!,
                                selectedClass,
                                selectedDepartment,
                                totalSemesters,
                                effectiveYear,
                                semestersData.toMap(), //semesterData
                                totalCredits
                            )
                        }

                        if (success) {
                            snackbarHostState.showSnackbar(
                                if (saveAsNew) "Curriculum saved as new" else "Curriculum updated successfully"
                            )
                            navController.popBackStack()
                        } else {
                            snackbarHostState.showSnackbar("Failed to save curriculum")
                        }
                    }
                },
                onCancel = {
                    showConfirmationDialog = false
                }
            )
        }
    }
}

@Composable
fun UnsavedChangesDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Unsaved Changes") },
        text = { Text("You have unsaved changes. Do you want to save them before leaving?") },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text("Discard")
            }
        }
    )
}

@Composable
fun SaveConfirmationDialog(
    saveAsNew: Boolean,
    onSaveAsNewChanged: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Save Curriculum",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "How would you like to save these changes?",
                    style = MaterialTheme.typography.bodyLarge
                )

                // Save options
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = !saveAsNew,
                            onClick = { onSaveAsNewChanged(false) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Overwrite Existing",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Replace the current curriculum with your changes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = saveAsNew,
                            onClick = { onSaveAsNewChanged(true) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Save as New",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Create a new curriculum with these changes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            if (saveAsNew) "Save as New" else "Overwrite",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Helper function to compare semester data
private fun semestersDataEquals(
    map1: Map<Int, List<CourseDetail>>,
    map2: Map<Int, List<CourseDetail>>
): Boolean {
    if (map1.size != map2.size) return false

    return map1.all { (key, courses1) ->
        val courses2 = map2[key]
        courses2?.size == courses1.size && courses1.zip(courses2).all { (c1, c2) ->
            c1.name == c2.name && c1.code == c2.code && c1.credits == c2.credits
        }
    }
}

// Data class to store original curriculum data
data class CurriculumData(
    val className: String,
    val department: String,
    val totalSemesters: Int,
    val effectiveYear: String,
    val semestersData: Map<Int, List<CourseDetail>>
)

// API functions
suspend fun getCurriculumById(id: String): JSONObject {
    // This is a placeholder - implement your actual API call
    val response = RetrofitClient.instance.getCurriculumById(id)
    return JSONObject(response.body()?.string() ?: "")
}

suspend fun updateCurriculum(
    id: String,
    className: String,
    department: String,
    totalSemesters: Int,
    effectiveYear: String,
    semesterData: Map<Int, List<CourseDetail>>,
    totalCredits: Double
): Boolean {
    return try {
        val curriculum = mapOf(
            "className" to className,
            "department" to department,
            "totalSemesters" to totalSemesters,
            "effectiveYear" to effectiveYear,
            "semesterData" to semesterData.mapKeys { it.key.toString() }
                .mapValues { (_, courses) ->
                    courses.map { course ->
                        mapOf(
                            "name" to course.name,
                            "code" to course.code,
                            "credits" to (course.credits.toDoubleOrNull() ?: 0)
                        )
                    }
                },
            "totalCredits" to totalCredits
        )

        val requestBody =
            JSONObject(curriculum).toString()
                .toRequestBody("application/json".toMediaType())
        println("JSON SENT FOR UPDATE = $curriculum")
        val response = RetrofitClient.instance.updateCurriculum(id, requestBody)
        println("UPDATE RESPONSE CODE = ${response.code()}")
        println("UPDATE RESPONSE BODY = ${response.errorBody()?.string() ?: response.body()?.string()}")
        response.isSuccessful
    } catch (e: Exception) {
        false
    }
}

suspend fun saveCurriculumAsNew(
    className: String,
    department: String,
    totalSemesters: Int,
    effectiveYear: String,
    semesterData: Map<Int, List<CourseDetail>>,
    totalCredits: Double
): Boolean {
    return try {
        val curriculum = mapOf(
            "className" to className,
            "department" to department,
            "totalSemesters" to totalSemesters,
            "effectiveYear" to effectiveYear,
            "semesterData" to semesterData.mapKeys { it.key.toString() }
                .mapValues { (_, courses) ->
                    courses.map { course ->
                        mapOf(
                            "name" to course.name,
                            "code" to course.code,
                            "credits" to (course.credits.toDoubleOrNull() ?: 0)
                        )
                    }
                },
            "totalCredits" to totalCredits
        )

        val requestBody =
            JSONObject(curriculum).toString()
                .toRequestBody("application/json".toMediaType())
        println("JSON SENT FOR CREATE = $curriculum")
        val response = RetrofitClient.instance.createCurriculum(requestBody)
        println("CREATE RESPONSE CODE = ${response.code()}")
        println("CREATE RESPONSE BODY = ${response.errorBody()?.string() ?: response.body()?.string()}")
        response.isSuccessful
    } catch (e: Exception) {
        false
    }
}