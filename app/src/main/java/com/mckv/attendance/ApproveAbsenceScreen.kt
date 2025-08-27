package com.mckv.attendance

//Jetpack Compose UI
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

//Navigation
import androidx.navigation.NavHostController

//JSON handling
import org.json.JSONArray
import org.json.JSONObject

//Networking (Retrofit)
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

//Logging
import android.util.Log
import com.mckv.attendance.model.RetrofitClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody


@Composable
fun ApproveAbsenceScreen(navController: NavHostController) {
    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
    val absenceData = savedStateHandle?.get<Map<String, Any>>("absenceData")
    val department = absenceData?.get("department") as? String ?: ""
    val selectedStudents = absenceData?.get("selectedStudents") as? List<Map<String, Any>> ?: emptyList()

    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Department (Read-only)
        /*OutlinedTextField(
            value = department,
            onValueChange = {},
            label = { Text("Department") },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )*/
        Text("Department: $department")

        // Selected Students (Read-only list)
        //Text("Selected Students:")
        LazyColumn {
            items(selectedStudents) { student ->
                Text(
                    text = "${student["name"] ?: "Unknown"} (${student["studentid"] ?: ""})"
                )
            }
        }

        // Date & Reason Inputs
        OutlinedTextField(
            value = fromDate,
            onValueChange = { fromDate = it },
            label = { Text("From Date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = toDate,
            onValueChange = { toDate = it },
            label = { Text("To Date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("Reason") },
            modifier = Modifier.fillMaxWidth()
        )

        // Approve Button
        Button(
            onClick = {
                val studentIdsArray = JSONArray().apply {
                    selectedStudents.forEach { student ->
                        put(student["_id"])   // ✅ Only _id goes to backend
                    }
                }
                val jsonObj = JSONObject().apply {
                    put("department", department)
                    //put("students", JSONArray(selectedStudents))
                    put("students", studentIdsArray)
                    put("fromDate", fromDate)
                    put("toDate", toDate)
                    put("reason", reason)
                }
                /*val requestBody = jsonObj.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaType())*/

                RetrofitClient.instance.approveLeave(
                    jsonObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                ).enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            Log.d("API", "Leave approved successfully")
                            navController.popBackStack()
                        } else {
                            Log.e("API", "Approval failed: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e("API", "Error approving leave", t)
                    }
                })
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Approve Absence")
        }
    }
}
