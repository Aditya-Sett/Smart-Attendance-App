package com.mckv.attendance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import com.mckv.attendance.data.remote.dto.request.ScheduleRequest
import com.mckv.attendance.data.remote.RetrofitClient
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

//import java.time.LocalTime
//import java.time.format.DateTimeFormatter


@Composable
fun ScheduleScreen(department: String,modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var allSchedules by remember { mutableStateOf<List<ScheduleRequest>>(emptyList()) }
    var filteredSchedules by remember { mutableStateOf<List<ScheduleRequest>>(emptyList()) }
    var dayFilter by remember { mutableStateOf("") }
    //var showSchedule by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val result = RetrofitClient.instance.getScheduleByDepartment(department)
                allSchedules = result
                filteredSchedules = result
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    if (isLoading) {
        CircularProgressIndicator()
    } else if (error != null) {
        Text("Error: $error")
    } else {
        Column(modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)) {
            OutlinedTextField(
                value = dayFilter,
                onValueChange = {
                    dayFilter = it
                    filteredSchedules = if (it.isBlank()) {
                        allSchedules
                    } else {
                        allSchedules.filter { schedule ->
                            schedule.day.contains(it, ignoreCase = true)
                        }
                    }
                },
                label = { Text("Search by day") }
            )

            LazyColumn {
                items(filteredSchedules) { schedule ->
                    Text(text = "${schedule.day}: ${schedule.subject} of GROUP ${schedule.group} at ${schedule.time} in ${schedule.room}")
                }
            }
        }
    }
}
