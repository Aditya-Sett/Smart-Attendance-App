/*package com.mckv.attendance.ui.screens

import android.graphics.Color
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.mckv.attendance.data.remote.RetrofitClient
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.component.text.textComponent
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import kotlinx.coroutines.launch
import org.json.JSONArray

// ─── Data classes ────────────────────────────────────────────────────────────

data class DepartmentResponse(
    val department: String,
    val academic_years: List<AcademicYear>
)

data class AcademicYear(
    val year: String,
    val semesters: List<Semester>
)

data class Semester(
    val semester: String,
    val overall_percentage: Double,
    val subjects: List<Subject>
)

data class Subject(
    val subject: String,
    val percentage: Double
)

// ─── ViewModel ───────────────────────────────────────────────────────────────

class ReportViewModel : ViewModel() {

    var selectedDepartment by mutableStateOf("CSEDS")
    var selectedYear by mutableStateOf("2025-26")
    var data by mutableStateOf<DepartmentResponse?>(null)
    var isLoading by mutableStateOf(false)

    init {
        fetchData(selectedDepartment)
    }

    fun fetchData(dept: String) {
        viewModelScope.launch {
            try {
                isLoading = true
                selectedDepartment = dept
                val response = RetrofitClient.analysisInstance.getDepartmentReport(dept)
                if (response.isSuccessful) {
                    val body = response.body()
                    val jsonString = body?.string()
                    data = if (!jsonString.isNullOrEmpty()) parseJson(jsonString) else null
                } else {
                    data = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
}

// ─── Screens ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(navController: NavController, viewModel: ReportViewModel) {
    val data = viewModel.data
    val isLoading = viewModel.isLoading

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Analytics Dashboard", fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    SectionHeader("Department")
                    DepartmentRow(viewModel)
                }

                item {
                    data?.academic_years?.let {
                        SectionHeader("Academic Year")
                        YearRow(viewModel, it)
                    }
                }

                item { KPISection(data) }

                item {
                    GraphCard(viewModel)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

// ─── Small UI pieces ─────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
fun KPISection(data: DepartmentResponse?) {
    val allSemesterPcts = data?.academic_years
        ?.flatMap { it.semesters }
        ?.map { it.overall_percentage }

    val avg = allSemesterPcts?.average() ?: 0.0
    val highest = allSemesterPcts?.maxOrNull() ?: 0.0
    val lowest = allSemesterPcts?.minOrNull() ?: 0.0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        KpiCard("Average", "${avg.toInt()}%", Modifier.weight(1f))
        KpiCard("Highest", "${highest.toInt()}%", Modifier.weight(1f))
        KpiCard("Lowest", "${lowest.toInt()}%", Modifier.weight(1f))
    }
}

@Composable
fun KpiCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun DepartmentRow(viewModel: ReportViewModel) {
    val departments = listOf("CSEDS", "CSE", "IT", "ECE")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(departments) { dept ->
            FilterChip(
                selected = dept == viewModel.selectedDepartment,
                onClick = { viewModel.fetchData(dept) },
                label = { Text(dept, fontWeight = FontWeight.Medium) },
                shape = RoundedCornerShape(50)
            )
        }
    }
}

@Composable
fun YearRow(viewModel: ReportViewModel, years: List<AcademicYear>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(years) { year ->
            FilterChip(
                selected = year.year == viewModel.selectedYear,
                onClick = { viewModel.selectedYear = year.year },
                label = { Text(year.year, fontWeight = FontWeight.Medium) },
                shape = RoundedCornerShape(50)
            )
        }
    }
}

// ─── Graph Card (main chart) ──────────────────────────────────────────────────


@Composable
fun GraphCard(viewModel: ReportViewModel) {
    val data = viewModel.data ?: return
    val year = data.academic_years.find { it.year == viewModel.selectedYear } ?: return

    if (year.semesters.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("No data available for this year")
        }
        return
    }

    var selectedSemester by remember { mutableStateOf<Semester?>(null) }
    val modelProducer = remember { ChartEntryModelProducer() }
    // ✅ Crash fix: gate rendering until producer has data
    var isModelReady by remember { mutableStateOf(false) }

    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val labelComponent = textComponent {
        color = labelColor
        textSizeSp = 12f
    }

    val horizontalAxisValueFormatter =
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
            year.semesters.getOrNull(value.toInt())?.semester ?: ""
        }

    val verticalAxisValueFormatter =
        AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
            "${value.toInt()}%"
        }

    LaunchedEffect(year) {
        isModelReady = false
        modelProducer.setEntries(
            year.semesters.mapIndexed { index, sem ->
                FloatEntry(index.toFloat(), sem.overall_percentage.toFloat())
            }
        )
        isModelReady = true
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Semester Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Percentage (%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.TopStart)
                )

                if (isModelReady) {
                    Chart(
                        chart = columnChart(
                            columns = listOf(
                                lineComponent(
                                    color = MaterialTheme.colorScheme.primary,
                                    thickness = 24.dp,
                                    shape = Shapes.roundedCornerShape(
                                        topLeftPercent = 40,
                                        topRightPercent = 40
                                    )
                                )
                            ),
                            spacing = 32.dp,
                            dataLabel = labelComponent,
                            axisValuesOverrider = AxisValuesOverrider.fixed(minY = 0f, maxY = 100f)
                        ),
                        chartModelProducer = modelProducer,
                        startAxis = rememberStartAxis(
                            valueFormatter = verticalAxisValueFormatter,
                            label = labelComponent
                        ),
                        bottomAxis = rememberBottomAxis(
                            valueFormatter = horizontalAxisValueFormatter,
                            label = labelComponent
                        ),
                        modifier = Modifier
                            .height(250.dp)
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                            // ✅ Tap gesture restored from your original code
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val itemWidth =
                                        size.width / (year.semesters.size.coerceAtLeast(1))
                                    val index = (offset.x / itemWidth).toInt()
                                    if (index in year.semesters.indices) {
                                        selectedSemester = year.semesters[index]
                                    }
                                }
                            }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .height(250.dp)
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            Text(
                "Semesters",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            )
        }
    }

    selectedSemester?.let {
        SubjectBottomSheet(it) { selectedSemester = null }
    }
}

// ─── Legacy / secondary graph composables ────────────────────────────────────

@Composable
fun ReportContent(viewModel: ReportViewModel, modifier: Modifier = Modifier) {
    val data = viewModel.data

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(text = "Department Analytics", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))
        DepartmentRow(viewModel)
        Spacer(modifier = Modifier.height(16.dp))
        data?.academic_years?.let { YearRow(viewModel, it) }
        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                if (data == null) {
                    Text("Loading...", modifier = Modifier.align(Alignment.Center))
                } else {
                    SemesterGraph(viewModel)
                }
            }
        }
    }
}

@Composable
fun SemesterGraph(viewModel: ReportViewModel) {
    val data = viewModel.data ?: return
    val year = data.academic_years.find { it.year == viewModel.selectedYear } ?: return
    if (year.semesters.isEmpty()) return

    val modelProducer = remember { ChartEntryModelProducer() }
    var isModelReady by remember { mutableStateOf(false) }

    val entries = remember(year.semesters) {
        year.semesters.mapIndexed { index, sem ->
            FloatEntry(index.toFloat(), sem.overall_percentage.toFloat())
        }
    }

    LaunchedEffect(entries) {
        isModelReady = false
        modelProducer.setEntries(entries)
        isModelReady = true
    }

    if (!isModelReady) return

    Chart(
        chart = columnChart(),
        chartModelProducer = modelProducer,
        modifier = Modifier.height(250.dp).fillMaxWidth()
    )
}

@Composable
fun SemesterBarChart(semesters: List<Semester>) {
    if (semesters.isEmpty()) return

    val modelProducer = remember { ChartEntryModelProducer() }
    var isModelReady by remember { mutableStateOf(false) }

    val entries = remember(semesters) {
        semesters.mapIndexed { index, sem ->
            FloatEntry(index.toFloat(), sem.overall_percentage.toFloat())
        }
    }

    LaunchedEffect(entries) {
        isModelReady = false
        modelProducer.setEntries(entries)
        isModelReady = true
    }

    if (!isModelReady) return

    Column {
        Chart(
            chart = columnChart(),
            chartModelProducer = modelProducer,
            modifier = Modifier.height(250.dp).fillMaxWidth()
        )
    }
}

// ─── Subject bottom sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectBottomSheet(
    semester: Semester,
    onClose: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        ) {
            Text(
                text = "${semester.semester} Semester: Subject Analysis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val modelProducer = remember { ChartEntryModelProducer() }
            var isModelReady by remember { mutableStateOf(false) }

            val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
            val labelComponent = textComponent {
                color = labelColor
                textSizeSp = 10f
            }

            val horizontalAxisValueFormatter =
                AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                    semester.subjects.getOrNull(value.toInt())?.subject ?: ""
                }

            val verticalAxisValueFormatter =
                AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
                    "${value.toInt()}%"
                }

            val entries = remember(semester) {
                semester.subjects.mapIndexed { index, sub ->
                    FloatEntry(index.toFloat(), sub.percentage.toFloat())
                }
            }

            LaunchedEffect(entries) {
                isModelReady = false
                modelProducer.setEntries(entries)
                isModelReady = true
            }

            // ✅ FIX: Only render Chart when producer actually has data
            if (isModelReady) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Score (%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align(Alignment.TopStart)
                    )

                    Chart(
                        chart = columnChart(
                            columns = listOf(
                                lineComponent(
                                    color = MaterialTheme.colorScheme.secondary,
                                    thickness = 20.dp,
                                    shape = Shapes.roundedCornerShape(
                                        topLeftPercent = 40,
                                        topRightPercent = 40
                                    )
                                )
                            ),
                            spacing = 24.dp,
                            dataLabel = labelComponent,
                            axisValuesOverrider = AxisValuesOverrider.fixed(minY = 0f, maxY = 100f)
                        ),
                        chartModelProducer = modelProducer,
                        startAxis = rememberStartAxis(
                            valueFormatter = verticalAxisValueFormatter,
                            label = labelComponent
                        ),
                        bottomAxis = rememberBottomAxis(
                            valueFormatter = horizontalAxisValueFormatter,
                            label = labelComponent,
                            labelRotationDegrees = 45f
                        ),
                        modifier = Modifier
                            .height(300.dp)
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                    )
                }

                Text(
                    text = "Subjects",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 1.dp)
                )
            } else {
                Box(
                    modifier = Modifier.height(300.dp).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

fun getBarColor(value: Double): Int {
    return when {
        value < 75 -> Color.parseColor("#EF5350")
        value < 85 -> Color.parseColor("#FFCA28")
        else -> Color.parseColor("#66BB6A")
    }
}

fun parseJson(json: String): DepartmentResponse {
    val obj = org.json.JSONObject(json)
    val yearsArray = obj.optJSONArray("academic_years") ?: JSONArray()
    val yearsList = mutableListOf<AcademicYear>()

    for (i in 0 until yearsArray.length()) {
        val yearObj = yearsArray.optJSONObject(i) ?: continue
        val semArray = yearObj.optJSONArray("semesters") ?: JSONArray()
        val semList = mutableListOf<Semester>()

        for (j in 0 until semArray.length()) {
            val semObj = semArray.optJSONObject(j) ?: continue
            val subArray = semObj.optJSONArray("subjects") ?: JSONArray()
            val subList = mutableListOf<Subject>()

            for (k in 0 until subArray.length()) {
                val subObj = subArray.optJSONObject(k) ?: continue
                subList.add(
                    Subject(
                        subject = subObj.optString("subject"),
                        percentage = subObj.optDouble("percentage")
                    )
                )
            }

            semList.add(
                Semester(
                    semester = semObj.optString("semester"),
                    overall_percentage = semObj.optDouble("overall_percentage"),
                    subjects = subList
                )
            )
        }

        yearsList.add(
            AcademicYear(
                year = yearObj.optString("year"),
                semesters = semList
            )
        )
    }

    return DepartmentResponse(
        department = obj.optString("department"),
        academic_years = yearsList
    )
}*/
/*package com.mckv.attendance.ui.screens

import android.graphics.Color
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.mckv.attendance.data.remote.RetrofitClient
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.component.text.textComponent
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import kotlinx.coroutines.launch
import org.json.JSONArray

// ─── Data classes ─────────────────────────────────────────────

data class DepartmentResponse(
    val department: String,
    val academic_years: List<AcademicYear>
)

data class AcademicYear(
    val year: String,
    val semesters: List<Semester>
)

data class Semester(
    val semester: String,
    val overall_percentage: Double,
    val subjects: List<Subject>
)

data class Subject(
    val subject: String,
    val percentage: Double
)

// ─── ViewModel ────────────────────────────────────────────────

class ReportViewModel : ViewModel() {

    var selectedDepartment by mutableStateOf("CSEDS")
    var selectedYear by mutableStateOf("2025-26")
    var data by mutableStateOf<DepartmentResponse?>(null)
    var isLoading by mutableStateOf(false)

    init {
        fetchData(selectedDepartment)
    }

    fun fetchData(dept: String) {
        viewModelScope.launch {
            try {
                isLoading = true
                selectedDepartment = dept
                val response = RetrofitClient.analysisInstance.getDepartmentReport(dept)
                if (response.isSuccessful) {
                    val body = response.body()
                    val jsonString = body?.string()
                    data = if (!jsonString.isNullOrEmpty()) parseJson(jsonString) else null
                } else {
                    data = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
}

// ─── Screen ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(navController: NavController, viewModel: ReportViewModel) {

    val data = viewModel.data
    val isLoading = viewModel.isLoading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics Dashboard", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize()) {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                item {
                    SectionHeader("Department")
                    DepartmentRow(viewModel)
                }

                item {
                    data?.academic_years?.let {
                        SectionHeader("Academic Year")
                        YearRow(viewModel, it)
                    }
                }

                item { KPISection(data) }

                item { GraphCard(viewModel) }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

// ─── UI Components ────────────────────────────────────────────

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(8.dp)
    )
}

@Composable
fun KPISection(data: DepartmentResponse?) {

    val values = data?.academic_years
        ?.flatMap { it.semesters }
        ?.map { it.overall_percentage } ?: emptyList()

    val avg = if (values.isNotEmpty()) values.average().toInt() else 0
    val max = values.maxOrNull()?.toInt() ?: 0
    val min = values.minOrNull()?.toInt() ?: 0

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KpiCard("Average", "$avg%", Modifier.weight(1f))
        KpiCard("Highest", "$max%", Modifier.weight(1f))
        KpiCard("Lowest", "$min%", Modifier.weight(1f))
    }
}

@Composable
fun KpiCard(title: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(title)
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DepartmentRow(viewModel: ReportViewModel) {
    val departments = listOf("CSEDS", "CSE", "IT", "ECE")

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(departments) {
            FilterChip(
                selected = it == viewModel.selectedDepartment,
                onClick = { viewModel.fetchData(it) },
                label = { Text(it) }
            )
        }
    }
}

@Composable
fun YearRow(viewModel: ReportViewModel, years: List<AcademicYear>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(years) {
            FilterChip(
                selected = it.year == viewModel.selectedYear,
                onClick = { viewModel.selectedYear = it.year },
                label = { Text(it.year) }
            )
        }
    }
}

// ─── FIXED GRAPH ──────────────────────────────────────────────

@Composable
fun GraphCard(viewModel: ReportViewModel) {

    //val data = viewModel.data ?: return
    val data = viewModel.data

    if (data == null || data.academic_years.isEmpty()) {
        Text("Loading...")
        return
    }

    val year = data.academic_years.find { it.year == viewModel.selectedYear }

    if (year == null || year.semesters.isEmpty()) {
        Text("No data available")
        return
    }


    val modelProducer = remember { ChartEntryModelProducer() }
    var selectedSemester by remember { mutableStateOf<Semester?>(null) }

    LaunchedEffect(year) {
        if (year.semesters.isNotEmpty()) {
            modelProducer.setEntries(
                year.semesters.mapIndexed { index, sem ->
                    FloatEntry(index.toFloat(), sem.overall_percentage.toFloat())
                }
            )
        }
    }

    // ✅ KEEP multiple colors per bar
    val columnComponents = year.semesters.map { sem ->
        lineComponent(
            color = androidx.compose.ui.graphics.Color(
                getBarColor(sem.overall_percentage)
            ),
            thickness = 24.dp,
            shape = Shapes.roundedCornerShape(
                topLeftPercent = 40,
                topRightPercent = 40
            )
        )
    }

    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    val labelComponent = textComponent {
        color = labelColor
        textSizeSp = 12f
    }

    val horizontalAxisValueFormatter =
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
            year.semesters.getOrNull(value.toInt())?.semester ?: ""
        }

    val verticalAxisValueFormatter =
        AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
            "${value.toInt()}%"
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text("Semester Performance", fontWeight = FontWeight.Bold)

            // ✅ YOUR ORIGINAL GRAPH (UNCHANGED)
            Chart(
                chart = columnChart(
                    columns = columnComponents,
                    spacing = 32.dp,
                    dataLabel = labelComponent,
                    axisValuesOverrider = AxisValuesOverrider.fixed(
                        minY = 0f,
                        maxY = 100f
                    )
                ),
                chartModelProducer = modelProducer,
                startAxis = rememberStartAxis(
                    valueFormatter = verticalAxisValueFormatter,
                    label = labelComponent
                ),
                bottomAxis = rememberBottomAxis(
                    valueFormatter = horizontalAxisValueFormatter,
                    label = labelComponent
                ),
                modifier = Modifier
                    .height(250.dp)
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val itemWidth = size.width / (year.semesters.size.coerceAtLeast(1))
                            val index = (offset.x / itemWidth).toInt()
                            if (index in year.semesters.indices) {
                               selectedSemester = year.semesters[index]
                                            }
                                        }
                                  }
            )
            selectedSemester?.let { semester ->

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "${semester.semester} Semester: Subject Analysis",
                    fontWeight = FontWeight.Bold
                )

                // ✅ 1. CREATE modelProducer HERE
                val subjectModelProducer = remember { ChartEntryModelProducer() }

                // ✅ 2. ADD LaunchedEffect HERE (RIGHT AFTER modelProducer)
                LaunchedEffect(semester) {
                    if (semester.subjects.isNotEmpty()) {
                        subjectModelProducer.setEntries(
                            semester.subjects.mapIndexed { index, sub ->
                                FloatEntry(index.toFloat(), sub.percentage.toFloat())
                            }
                        )
                    }
                }

                // ✅ 3. Create columns
                val subjectColumns = semester.subjects.map { sub ->
                    lineComponent(
                        color = androidx.compose.ui.graphics.Color(
                            getBarColor(sub.percentage)
                        ),
                        thickness = 20.dp,
                        shape = Shapes.roundedCornerShape(
                            topLeftPercent = 40,
                            topRightPercent = 40
                        )
                    )
                }

                // ✅ 4. Subject Chart
                Chart(
                    chart = columnChart(
                        columns = subjectColumns,
                        spacing = 24.dp,
                        dataLabel = labelComponent
                    ),
                    chartModelProducer = subjectModelProducer,
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = AxisValueFormatter { value, _ ->
                            semester.subjects.getOrNull(value.toInt())?.subject ?: ""
                        },
                        label = labelComponent
                    ),
                    startAxis = rememberStartAxis(
                        valueFormatter = verticalAxisValueFormatter,
                        label = labelComponent
                    ),
                    modifier = Modifier
                        .height(220.dp)
                        .fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ✅ ONLY THIS PART WAS NEEDED (LEGEND)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem("Low", "#EF5350")
                LegendItem("Moderate", "#FFA726")
                LegendItem("High", "#66BB6A")
            }
        }
    }
}
@Composable
fun LegendItem(label: String, colorHex: String) {

    val color = try {
        androidx.compose.ui.graphics.Color(Color.parseColor(colorHex))
    } catch (e: Exception) {
        androidx.compose.ui.graphics.Color.Gray
    }

    Row(verticalAlignment = Alignment.CenterVertically) {

        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color, shape = RoundedCornerShape(50))
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(text = label)
    }
}
// ─── Color Logic ──────────────────────────────────────────────

fun getBarColor(value: Double): Int {
    return when {
        value < 50 -> Color.parseColor("#EF5350")
        value < 75 -> Color.parseColor("#FFA726")
        else -> Color.parseColor("#66BB6A")
    }
}

// ─── JSON Parser ──────────────────────────────────────────────

fun parseJson(json: String): DepartmentResponse {
    val obj = org.json.JSONObject(json)
    val yearsArray = obj.optJSONArray("academic_years") ?: JSONArray()
    val yearsList = mutableListOf<AcademicYear>()

    for (i in 0 until yearsArray.length()) {
        val yearObj = yearsArray.optJSONObject(i) ?: continue
        val semArray = yearObj.optJSONArray("semesters") ?: JSONArray()
        val semList = mutableListOf<Semester>()

        for (j in 0 until semArray.length()) {
            val semObj = semArray.optJSONObject(j) ?: continue
            val subArray = semObj.optJSONArray("subjects") ?: JSONArray()
            val subList = mutableListOf<Subject>()

            for (k in 0 until subArray.length()) {
                val subObj = subArray.optJSONObject(k) ?: continue
                subList.add(
                    Subject(
                        subject = subObj.optString("subject"),
                        percentage = subObj.optDouble("percentage")
                    )
                )
            }

            semList.add(
                Semester(
                    semester = semObj.optString("semester"),
                    overall_percentage = semObj.optDouble("overall_percentage"),
                    subjects = subList
                )
            )
        }

        yearsList.add(
            AcademicYear(
                year = yearObj.optString("year"),
                semesters = semList
            )
        )
    }

    return DepartmentResponse(
        department = obj.optString("department"),
        academic_years = yearsList
    )
}*/
package com.mckv.attendance.ui.screens

import android.graphics.Color
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.mckv.attendance.data.remote.RetrofitClient
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.component.text.textComponent
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import kotlinx.coroutines.launch
import org.json.JSONArray

// ─── Data classes ─────────────────────────────────────────────

data class DepartmentResponse(
    val department: String,
    val academic_years: List<AcademicYear>
)

data class AcademicYear(
    val year: String,
    val semesters: List<Semester>
)

data class Semester(
    val semester: String,
    val overall_percentage: Double,
    val subjects: List<Subject>
)

data class Subject(
    val subject: String,
    val percentage: Double
)

// ─── ViewModel ────────────────────────────────────────────────

class ReportViewModel : ViewModel() {

    var selectedDepartment by mutableStateOf("CSEDS")
    var selectedYear by mutableStateOf("2025-26")
    var data by mutableStateOf<DepartmentResponse?>(null)
    var isLoading by mutableStateOf(false)

    init {
        fetchData(selectedDepartment)
    }

    fun fetchData(dept: String) {
        viewModelScope.launch {
            try {
                isLoading = true
                selectedDepartment = dept
                val response = RetrofitClient.analysisInstance.getDepartmentReport(dept)
                if (response.isSuccessful) {
                    val body = response.body()
                    val jsonString = body?.string()
                    data = if (!jsonString.isNullOrEmpty()) parseJson(jsonString) else null
                    // FIX: Auto-select the first available year after data loads
                    data?.academic_years?.firstOrNull()?.let { firstYear ->
                        if (selectedYear.isEmpty() || data?.academic_years?.none { it.year == selectedYear } == true) {
                            selectedYear = firstYear.year
                        }
                    }
                } else {
                    data = null
                }
            } catch (e: Exception) {
                // FIX: Always reset isLoading in catch block too (was missing before)
                e.printStackTrace()
                data = null
            } finally {
                // FIX: isLoading is now guaranteed to reset even on exception
                isLoading = false
            }
        }
    }
}

// ─── Screen ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(navController: NavController, viewModel: ReportViewModel) {

    val data = viewModel.data
    val isLoading = viewModel.isLoading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics Dashboard", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize()) {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                item {
                    SectionHeader("Department")
                    DepartmentRow(viewModel)
                }

                item {
                    data?.academic_years?.let {
                        SectionHeader("Academic Year")
                        YearRow(viewModel, it)
                    }
                }

                item { KPISection(data) }

                item { GraphCard(viewModel) }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

// ─── UI Components ────────────────────────────────────────────

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(8.dp)
    )
}

@Composable
fun KPISection(data: DepartmentResponse?) {
    // FIX: Safe null-check with safe default list to avoid crash during recomposition
    val values = data?.academic_years
        ?.flatMap { it.semesters }
        ?.map { it.overall_percentage }
        .orEmpty()

    val avg = if (values.isNotEmpty()) values.average().toInt() else 0
    val max = if (values.isNotEmpty()) values.max().toInt() else 0
    val min = if (values.isNotEmpty()) values.min().toInt() else 0

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KpiCard("Average", "$avg%", Modifier.weight(1f))
        KpiCard("Highest", "$max%", Modifier.weight(1f))
        KpiCard("Lowest", "$min%", Modifier.weight(1f))
    }
}

@Composable
fun KpiCard(title: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(title)
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DepartmentRow(viewModel: ReportViewModel) {
    val departments = listOf("CSEDS", "CSE", "IT", "ECE")

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(departments) {
            FilterChip(
                selected = it == viewModel.selectedDepartment,
                onClick = { viewModel.fetchData(it) },
                label = { Text(it) }
            )
        }
    }
}

@Composable
fun YearRow(viewModel: ReportViewModel, years: List<AcademicYear>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(years) {
            FilterChip(
                selected = it.year == viewModel.selectedYear,
                onClick = { viewModel.selectedYear = it.year },
                label = { Text(it.year) }
            )
        }
    }
}

// ─── GRAPH ────────────────────────────────────────────────────

@Composable
fun GraphCard(viewModel: ReportViewModel) {

    val data = viewModel.data

    if (data == null || data.academic_years.isEmpty()) {
        Text("Loading...")
        return
    }

    val year = data.academic_years.find { it.year == viewModel.selectedYear }

    if (year == null || year.semesters.isEmpty()) {
        Text("No data available")
        return
    }

    val modelProducer = remember { ChartEntryModelProducer() }
    val subjectModelProducer = remember { ChartEntryModelProducer() }
    var selectedSemester by remember { mutableStateOf<Semester?>(null) }

    LaunchedEffect(year) {
        selectedSemester = null
        modelProducer.setEntries(
            year.semesters.mapIndexed { index, sem ->
                FloatEntry(index.toFloat(), sem.overall_percentage.toFloat())
            }
        )
    }

    LaunchedEffect(selectedSemester) {
        val semester = selectedSemester
        if (semester != null && semester.subjects.isNotEmpty()) {
            subjectModelProducer.setEntries(
                semester.subjects.mapIndexed { index, sub ->
                    FloatEntry(index.toFloat(), sub.percentage.toFloat())
                }
            )
        } else {
            // ✅ Prevent stale/crash state
            subjectModelProducer.setEntries(emptyList<FloatEntry>())
        }
    }

    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    val labelComponent = remember(labelColor) {
        textComponent {
            color = labelColor
            textSizeSp = 12f
        }
    }

    val verticalAxisValueFormatter =
        AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
            "${value.toInt()}%"
        }

    val horizontalAxisValueFormatter =
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
            year.semesters.getOrNull(value.toInt())?.semester ?: ""
        }

    val semesterBottomAxis = rememberBottomAxis(
        valueFormatter = horizontalAxisValueFormatter,
        label = labelComponent
    )

    val semesterStartAxis = rememberStartAxis(
        valueFormatter = verticalAxisValueFormatter,
        label = labelComponent
    )

    val subjectAxisFormatter = remember(selectedSemester) {
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
            selectedSemester?.subjects?.getOrNull(value.toInt())?.subject ?: ""
        }
    }

    val subjectBottomAxis = rememberBottomAxis(
        valueFormatter = subjectAxisFormatter,
        label = labelComponent
    )

    val subjectStartAxis = rememberStartAxis(
        valueFormatter = verticalAxisValueFormatter,
        label = labelComponent
    )

    val columnComponents = year.semesters.map { sem ->
        lineComponent(
            color = androidx.compose.ui.graphics.Color(getBarColor(sem.overall_percentage)),
            thickness = 24.dp,
            shape = Shapes.roundedCornerShape(topLeftPercent = 40, topRightPercent = 40)
        )
    }

    val subjectColumns = (selectedSemester?.subjects ?: emptyList()).map { sub ->
        lineComponent(
            color = androidx.compose.ui.graphics.Color(getBarColor(sub.percentage)),
            thickness = 20.dp,
            shape = Shapes.roundedCornerShape(topLeftPercent = 40, topRightPercent = 40)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text("Semester Performance", fontWeight = FontWeight.Bold)

            Chart(
                chart = columnChart(
                    columns = columnComponents,
                    spacing = 32.dp,
                    dataLabel = labelComponent,
                    axisValuesOverrider = AxisValuesOverrider.fixed(minY = 0f, maxY = 100f)
                ),
                chartModelProducer = modelProducer,
                startAxis = semesterStartAxis,
                bottomAxis = semesterBottomAxis,
                modifier = Modifier
                    .height(250.dp)
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .pointerInput(year.semesters.size) {
                        detectTapGestures { offset ->

                            val count = year.semesters.size
                            if (count == 0 || size.width <= 0f) return@detectTapGestures

                            val itemWidth = size.width / count

                            val rawIndex = (offset.x / itemWidth).toInt()

                            // ✅ CRASH FIX: Clamp index safely
                            val safeIndex = rawIndex.coerceIn(0, count - 1)

                            selectedSemester = year.semesters.getOrNull(safeIndex)
                        }
                    }
            )

            if (selectedSemester != null && subjectColumns.isNotEmpty()) {

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "${selectedSemester!!.semester} Semester: Subject Analysis",
                    fontWeight = FontWeight.Bold
                )

                Chart(
                    chart = columnChart(
                        columns = subjectColumns,
                        spacing = 24.dp,
                        dataLabel = labelComponent,
                        axisValuesOverrider = AxisValuesOverrider.fixed(minY = 0f, maxY = 100f)
                    ),
                    chartModelProducer = subjectModelProducer,
                    bottomAxis = subjectBottomAxis,
                    startAxis = subjectStartAxis,
                    modifier = Modifier
                        .height(220.dp)
                        .fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem("Low", "#EF5350")
                LegendItem("Moderate", "#FFA726")
                LegendItem("High", "#66BB6A")
            }
        }
    }
}

// ─── Legend ───────────────────────────────────────────────────

@Composable
fun LegendItem(label: String, colorHex: String) {
    val color = try {
        androidx.compose.ui.graphics.Color(Color.parseColor(colorHex))
    } catch (e: Exception) {
        androidx.compose.ui.graphics.Color.Gray
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color, shape = RoundedCornerShape(50))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label)
    }
}

// ─── Color Logic ──────────────────────────────────────────────

fun getBarColor(value: Double): Int {
    return when {
        value < 50 -> Color.parseColor("#EF5350")
        value < 75 -> Color.parseColor("#FFA726")
        else -> Color.parseColor("#66BB6A")
    }
}

// ─── JSON Parser ──────────────────────────────────────────────

fun parseJson(json: String): DepartmentResponse {
    val obj = org.json.JSONObject(json)
    val yearsArray = obj.optJSONArray("academic_years") ?: JSONArray()
    val yearsList = mutableListOf<AcademicYear>()

    for (i in 0 until yearsArray.length()) {
        val yearObj = yearsArray.optJSONObject(i) ?: continue
        val semArray = yearObj.optJSONArray("semesters") ?: JSONArray()
        val semList = mutableListOf<Semester>()

        for (j in 0 until semArray.length()) {
            val semObj = semArray.optJSONObject(j) ?: continue
            val subArray = semObj.optJSONArray("subjects") ?: JSONArray()
            val subList = mutableListOf<Subject>()

            for (k in 0 until subArray.length()) {
                val subObj = subArray.optJSONObject(k) ?: continue
                subList.add(
                    Subject(
                        subject = subObj.optString("subject"),
                        percentage = subObj.optDouble("percentage")
                    )
                )
            }

            semList.add(
                Semester(
                    semester = semObj.optString("semester"),
                    overall_percentage = semObj.optDouble("overall_percentage"),
                    subjects = subList
                )
            )
        }

        yearsList.add(
            AcademicYear(
                year = yearObj.optString("year"),
                semesters = semList
            )
        )
    }

    return DepartmentResponse(
        // FIX: was previously parsing "department" key correctly but now explicitly
        // uses optString with a safe fallback instead of relying on implicit empty string
        department = obj.optString("department", "Unknown"),
        academic_years = yearsList
    )
}