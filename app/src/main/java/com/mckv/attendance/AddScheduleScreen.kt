package com.mckv.attendance

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mckv.attendance.model.RetrofitClient
import com.mckv.attendance.model.Schedule
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember


@Composable
fun AddScheduleScreen(navController: NavHostController) {
    var department by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(value = department, onValueChange = { department = it }, label = { Text("Department") })
        OutlinedTextField(value = day, onValueChange = { day = it }, label = { Text("Day") })
        OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time") })
        OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") })
        OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("Room") })
        OutlinedTextField(value = group, onValueChange = { group = it }, label = { Text("Group") })

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            scope.launch {
                // ✅ Validation block FIRST
                if (department.isBlank() || day.isBlank() || time.isBlank() ||
                    subject.isBlank() || room.isBlank() || group.isBlank()) {
                    Toast.makeText(context, "⚠️ Please fill all fields before submitting.", Toast.LENGTH_LONG).show()
                    return@launch  // ⛔ Stop further execution
                }
                try {
                    // ✅ Build the schedule object
                    val schedule = Schedule(department, day, time, subject, room, group)
                    // ✅ DEBUG: Print the JSON being sent
                    println("📦 Sending Schedule: ${com.google.gson.Gson().toJson(schedule)}")
                    // ✅ Call the API
                    val response = RetrofitClient.instance.addSchedule(schedule)
                    if (response.isSuccessful) {
                        Toast.makeText(context, "✅ Schedule Added", Toast.LENGTH_SHORT).show()
                        navController.popBackStack() // Go back
                    } else {
                        Toast.makeText(context, "❌ Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Exception: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }) {
            Text("Add Schedule")
        }
    }
}
