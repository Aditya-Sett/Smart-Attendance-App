package com.mckv.attendance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.runtime.*
import com.mckv.attendance.data.remote.RetrofitClient
import kotlinx.coroutines.launch
import com.mckv.attendance.data.remote.dto.response.ClassroomResponse
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material3.TopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomListScreen(navController: NavHostController) {
    var classrooms by remember { mutableStateOf<List<ClassroomResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.instance.getClassrooms()
            if (response.isSuccessful) {
                classrooms = response.body() ?: emptyList()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error fetching classrooms", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Classrooms") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("addClassroom") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Classroom")
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (classrooms.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No classrooms found")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(classrooms) { classroom ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Room: ${classroom.number}", style = MaterialTheme.typography.titleMedium)
                            Text("Corners: ${classroom.coordinates.size}")
                        }
                    }
                }
            }
        }
    }
}
