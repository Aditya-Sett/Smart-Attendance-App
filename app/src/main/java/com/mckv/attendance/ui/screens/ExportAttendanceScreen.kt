package com.mckv.attendance.ui.screens

/*import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mckv.attendance.data.remote.api.ApiService
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import java.text.SimpleDateFormat
import java.util.*/


import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
//import androidx.camera.camera2.pipe.core.Log
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import com.mckv.attendance.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

@Composable
fun ExportAttendanceScreen(navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var department by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var className by remember { mutableStateOf("") }
    var academicYear by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                Toast.makeText(context, "Storage permission required!", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Export Attendance Report",
            fontSize = 22.sp,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = department,
            onValueChange = { department = it },
            label = { Text("Department") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        )

        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        )

        OutlinedTextField(
            value = className,
            onValueChange = { className = it },
            label = { Text("Class Year") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        )

        OutlinedTextField(
            value = academicYear,
            onValueChange = { academicYear = it },
            label = { Text("Academic Year") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (department.isNotEmpty() && subject.isNotEmpty()
                    && className.isNotEmpty() && academicYear.isNotEmpty()
                ) {
                    coroutineScope.launch(Dispatchers.IO) {
                        isLoading = true
                        try {
                            Log.d("ExportURL", "Exporting -> $department / $subject / $className / $academicYear")
                            val call = RetrofitClient.instance.exportAttendanceExcel(
                                department, subject, className, academicYear
                            )
                            call.enqueue(object : Callback<ResponseBody> {
                                override fun onResponse(
                                    call: Call<ResponseBody>,
                                    response: Response<ResponseBody>
                                ) {
                                    isLoading = false
                                    if (response.isSuccessful && response.body() != null) {
                                        val bytes = response.body()!!.bytes()
                                        val file = File(
                                            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                                            "Attendance_${department}_${className}_${subject}_${academicYear}.xlsx"
                                        )
                                        FileOutputStream(file).use { it.write(bytes) }

                                        Toast.makeText(
                                            context,
                                            "File saved to: ${file.absolutePath}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        openFile(context, file)
                                    } else {
                                        Log.d("ExportResponse", "Code: ${response.code()} Message: ${response.message()}")
                                        Toast.makeText(
                                            context,
                                            //Log.d("ExportResponse", "Code: ${response.code()} Message: ${response.message()}")
                                            "Failed to download Excel",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }

                                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                    isLoading = false
                                    Toast.makeText(
                                        context,
                                        "Error: ${t.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            })
                        } catch (e: Exception) {
                            isLoading = false
                            launch(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Error: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Generate Excel")
            }
        }
    }
}

private fun openFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Open Excel file"))
}

