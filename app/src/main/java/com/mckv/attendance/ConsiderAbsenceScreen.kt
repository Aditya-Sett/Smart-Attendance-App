package com.mckv.attendance

// Jetpack Compose UI
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Navigation
import androidx.navigation.NavHostController

// Retrofit & networking
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// JSON handling
import org.json.JSONArray
import org.json.JSONObject

// Logging
import android.util.Log
import com.mckv.attendance.model.RetrofitClient


@Composable
fun ConsiderAbsenceScreen(navController: NavHostController) {
    var department by remember { mutableStateOf("") }
    var students by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedStudents by remember { mutableStateOf<List<String>>(emptyList()) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = department,
            onValueChange = { department = it },
            label = { Text("Department") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                RetrofitClient.instance.getStudentsByDepartment(department).enqueue(object: Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            val jsonStr = response.body()?.string()
                            val jsonArray = JSONArray(jsonStr)
                            val tempList = mutableListOf<Map<String, Any>>()
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                tempList.add(obj.toMap())
                            }
                            students = tempList
                        }
                    }
                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e("API", "Error fetching students", t)
                    }
                })
            },
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("Fetch Students")
        }

        LazyColumn {
            items(students) { student ->
                val id = student["_id"].toString()
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedStudents.contains(id),
                        onCheckedChange = { checked ->
                            selectedStudents = if (checked) {
                                selectedStudents + id
                            } else {
                                selectedStudents - id
                            }
                        }
                    )
                    Text("${student["name"] ?: "Unknown"} (${student["studentid"] ?: ""})")
                }
            }
        }

        Button(
            onClick = {
                val dataToPass = mapOf(
                    "department" to department,
                    //"selectedStudents" to selectedStudents
                    "selectedStudents" to students.filter { selectedStudents.contains(it["_id"].toString()) }
                )
                navController.currentBackStackEntry?.savedStateHandle?.set(
                    "absenceData", dataToPass
                )
                navController.navigate("approve_absence")
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Proceed")
        }
    }
}

fun JSONObject.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    keys().forEach { key -> map[key] = get(key) }
    return map
}
