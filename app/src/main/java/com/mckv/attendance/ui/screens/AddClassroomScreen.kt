package com.mckv.attendance.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.foundation.clickable
import com.mckv.attendance.utils.getCurrentLocation
import com.mckv.attendance.data.remote.dto.dto_utils.Coordinate
import androidx.compose.material3.TopAppBar
import com.mckv.attendance.data.remote.RetrofitClient
import androidx.compose.foundation.lazy.*
import com.mckv.attendance.data.remote.dto.request.ClassroomRequest
import androidx.compose.runtime.*
import com.mckv.attendance.data.remote.dto.dto_utils.Polygon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClassroomScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var roomNumber by remember { mutableStateOf("") }
    var coordinates by remember { mutableStateOf(listOf<Coordinate>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Classroom") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = roomNumber,
                onValueChange = { roomNumber = it },
                label = { Text("Classroom Number") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    getCurrentLocation(context) { lat, lon ->
                        coordinates = coordinates + Coordinate(lat, lon)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Fetch Location (Go to Corner)")
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(coordinates.size) { i ->
                    Text("Corner ${i + 1}: ${coordinates[i].lat}, ${coordinates[i].lon}")
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (roomNumber.isNotBlank() && coordinates.size >= 3) {
                        // Convert to GeoJSON format
                        val coords = coordinates.map { listOf(it.lon, it.lat) }.toMutableList()
                        if (coords.first() != coords.last()) {
                            coords.add(coords.first()) // close polygon
                        }

                        val request = ClassroomRequest(
                            roomNumber,
                            Polygon("Polygon", listOf(coords))
                        )

                        scope.launch {
                            try {
                                val response = RetrofitClient.instance.addClassroom(request)
                                withContext(Dispatchers.Main) {
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "Saved successfully", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    } else {
                                        Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(context, "Add at least 3 corners", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = roomNumber.isNotBlank() && coordinates.size >= 3
            ) {
                Text("Save Classroom")
            }
        }
    }
}
