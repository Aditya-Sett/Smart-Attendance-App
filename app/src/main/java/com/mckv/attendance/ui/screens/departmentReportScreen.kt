package com.mckv.attendance.ui.screens

import android.annotation.SuppressLint
import android.graphics.Color
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
//import androidx.wear.compose.material.Chip
import com.mckv.attendance.data.remote.RetrofitClient
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import okhttp3.ResponseBody
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider

import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
// For Vico 2.x
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.component.text.textComponent

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

class ReportViewModel : ViewModel() {

    var selectedDepartment by mutableStateOf("CSEDS")
    var selectedYear by mutableStateOf("2025-26")

    var data by mutableStateOf<DepartmentResponse?>(null)

    init {
        fetchData(selectedDepartment)
    }

    fun fetchData(dept: String) {
        viewModelScope.launch {
            try {
                selectedDepartment = dept
                val response = RetrofitClient.analysisInstance.getDepartmentReport(dept)

                if (response.isSuccessful) {
                    val jsonString = response.body()?.string() ?: ""
                    data = parseJson(jsonString)
                }
            } catch (e:Exception) {
                e.printStackTrace()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(navController: NavController, viewModel: ReportViewModel) {
    val data = viewModel.data

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Analytics Dashboard",
                        fontWeight = FontWeight.SemiBold
                    )
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

            item {
                KPISection(data)
            }

            item {
                GraphCard(viewModel)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

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
    val avg = data?.academic_years
        ?.flatMap { it.semesters }
        ?.map { it.overall_percentage }
        ?.average() ?: 0.0

    val highest = data?.academic_years
        ?.flatMap { it.semesters }
        ?.map { it.overall_percentage }
        ?.maxOrNull() ?: 0.0

    val lowest = data?.academic_years
        ?.flatMap { it.semesters }
        ?.map { it.overall_percentage }
        ?.minOrNull() ?: 0.0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        KpiCard("Average", "${avg.toInt()}%", Modifier.weight(1f))
        KpiCard("Highest", "${highest.toInt()}%", Modifier.weight(1f)) // Replace with dynamic calculation if available
        KpiCard("Lowest", "${lowest.toInt()}%", Modifier.weight(1f))  // Replace with dynamic calculation if available
    }
}

@Composable
fun KpiCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
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

    // Create the labels manually - this avoids "rememberTextComponent" issues
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val labelComponent = textComponent {
        color = labelColor
        textSizeSp = 12f
    }

    val horizontalAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        year.semesters.getOrNull(value.toInt())?.semester ?: ""
    }

    val verticalAxisValueFormatter = AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
        "${value.toInt()}%"
    }

    LaunchedEffect(year) {
        modelProducer.setEntries(
            year.semesters.mapIndexed { index, sem ->
                FloatEntry(index.toFloat(), sem.overall_percentage.toFloat())
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Semester Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Percentage (%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.TopStart)
                )

                Chart(
                    chart = columnChart(
                        columns = listOf(
                            lineComponent(
                                color = MaterialTheme.colorScheme.primary,
                                thickness = 24.dp,
                                shape = Shapes.roundedCornerShape(topLeftPercent = 40, topRightPercent = 40)
                            )
                        ),
                        spacing = 32.dp,
                        dataLabel = labelComponent, // Now using our safe component
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
            }
            Text(
                "Semesters",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
            )
        }
    }

    selectedSemester?.let {
        SubjectBottomSheet(it) { selectedSemester = null }
    }
}

@Composable
fun ReportContent(viewModel: ReportViewModel, modifier: Modifier = Modifier) {

    val data = viewModel.data

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // 🔹 Title
        Text(
            text = "Department Analytics",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 🔹 Departments
        DepartmentRow(viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        // 🔹 Years
        data?.academic_years?.let {
            YearRow(viewModel, it)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 🔹 Graph Card
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
fun DepartmentRow(viewModel: ReportViewModel) {
    val departments = listOf("CSEDS", "CSE", "IT", "ECE")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(departments) { dept ->
            FilterChip(
                selected = dept == viewModel.selectedDepartment,
                onClick = { viewModel.fetchData(dept) },
                label = { Text(dept, fontWeight = FontWeight.Medium) },
                shape = RoundedCornerShape(50) // Pill shape
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

@Composable
fun SemesterGraph(viewModel: ReportViewModel) {

    val data = viewModel.data ?: return

    val year = data.academic_years.find {
        it.year == viewModel.selectedYear
    } ?: return

    val modelProducer = remember { ChartEntryModelProducer() }

    LaunchedEffect(year) {
        modelProducer.setEntries(
            year.semesters.mapIndexed { index, sem ->
                FloatEntry(index.toFloat(), sem.overall_percentage.toFloat())
            }
        )
    }

    Chart(
        chart = columnChart(),
        chartModelProducer = modelProducer,
        modifier = Modifier
            .height(250.dp)
            .fillMaxWidth()
    )
}

@SuppressLint("RememberReturnType")
@Composable
fun SemesterBarChart(semesters: List<Semester>) {

    val modelProducer = remember { ChartEntryModelProducer() }

    LaunchedEffect(semesters) {
        modelProducer.setEntries(
            semesters.mapIndexed { index, sem ->
                FloatEntry(index.toFloat(), sem.overall_percentage.toFloat())
            }
        )
    }

    var selectedSemester by remember { mutableStateOf<Semester?>(null) }

    Column {

        Chart(
            chart = columnChart(),
            chartModelProducer = modelProducer,
            modifier = Modifier
                .height(250.dp)
                .fillMaxWidth(),
            /*onEntryClick = { entry ->
                selectedSemester = semesters[entry.x.toInt()]
            }*/

        )

        // This if block is for temporarily
        /*if (semesters.isNotEmpty()) {
            SubjectBottomSheet(semesters[0])
        }*/

        // 🔻 Subject Drill Down
        /*selectedSemester?.let {
            SubjectBottomSheet(it)
        }*/
    }
}

/*@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectBottomSheet(semester: Semester) {

    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = sheetState
    ) {

        Text(
            text = "${semester.semester} Semester",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        val modelProducer = remember { ChartEntryModelProducer() }

        LaunchedEffect(semester) {
            modelProducer.setEntries(
                semester.subjects.mapIndexed { index, sub ->
                    FloatEntry(index.toFloat(), sub.percentage.toFloat())
                }
            )
        }

        Chart(
            chart = columnChart(),
            chartModelProducer = modelProducer,
            modifier = Modifier
                .height(250.dp)
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}*/

fun getBarColor(value: Double): Int {
    return when {
        value < 75 -> Color.parseColor("#EF5350") // red
        value < 85 -> Color.parseColor("#FFCA28") // yellow
        else -> Color.parseColor("#66BB6A") // green
    }
}

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
            // Header
            Text(
                text = "${semester.semester} Semester: Subject Analysis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val modelProducer = remember { ChartEntryModelProducer() }

            // 1. Define Labels and Formatters
            val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
            val labelComponent = textComponent {
                color = labelColor
                textSizeSp = 10f // Slightly smaller for bottom sheet
            }

            // X-Axis shows Subject Names
            val horizontalAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                semester.subjects.getOrNull(value.toInt())?.subject ?: ""
            }

            // Y-Axis shows Percentage
            val verticalAxisValueFormatter = AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
                "${value.toInt()}%"
            }

            LaunchedEffect(semester) {
                modelProducer.setEntries(
                    semester.subjects.mapIndexed { index, sub ->
                        FloatEntry(index.toFloat(), sub.percentage.toFloat())
                    }
                )
            }

            // 2. Chart with Axes and Titles
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
                                color = MaterialTheme.colorScheme.secondary, // Different color for subjects
                                thickness = 20.dp,
                                shape = Shapes.roundedCornerShape(topLeftPercent = 40, topRightPercent = 40)
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
                        labelRotationDegrees = 45f // Rotate labels if subject names are long
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
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 1.dp)
            )
        }
    }
}

fun parseJson(json: String): DepartmentResponse {
    val obj = org.json.JSONObject(json)

    val yearsArray = obj.getJSONArray("academic_years")

    val yearsList = mutableListOf<AcademicYear>()

    for (i in 0 until yearsArray.length()) {
        val yearObj = yearsArray.getJSONObject(i)

        val semArray = yearObj.getJSONArray("semesters")
        val semList = mutableListOf<Semester>()

        for (j in 0 until semArray.length()) {
            val semObj = semArray.getJSONObject(j)

            val subArray = semObj.getJSONArray("subjects")
            val subList = mutableListOf<Subject>()

            for (k in 0 until subArray.length()) {
                val subObj = subArray.getJSONObject(k)

                subList.add(
                    Subject(
                        subject = subObj.getString("subject"),
                        percentage = subObj.getDouble("percentage")
                    )
                )
            }

            semList.add(
                Semester(
                    semester = semObj.getString("semester"),
                    overall_percentage = semObj.getDouble("overall_percentage"),
                    subjects = subList
                )
            )
        }

        yearsList.add(
            AcademicYear(
                year = yearObj.getString("year"),
                semesters = semList
            )
        )
    }

    return DepartmentResponse(
        department = obj.getString("department"),
        academic_years = yearsList
    )
}