package com.mckv.attendance.ui.screens


import android.content.Context
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
import java.util.*

@Composable
fun StudentsAttendanceSummaryScreen(apiService: ApiService, navController: NavHostController) {
    val context = LocalContext.current
    var department by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var studentList by remember { mutableStateOf<List<JSONObject>>(emptyList()) }

    Column(Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(16.dp)) {

        Text("Students Attendance History", fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = department,
            onValueChange = { department = it },
            label = { Text("Department") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        Button(
            onClick = {
                isLoading = true
                val call = apiService.getStudentsAttendanceSummary(department, subject)
                call.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        isLoading = false
                        if (response.isSuccessful) {
                            val body = response.body()?.string()
                            val json = JSONObject(body ?: "")
                            val array = json.getJSONArray("summary")

                            val result = mutableListOf<JSONObject>()
                            for (i in 0 until array.length()) {
                                result.add(array.getJSONObject(i))
                            }
                            studentList = result
                        } else {
                            Toast.makeText(context, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        isLoading = false
                        Toast.makeText(context, "Failed: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("Fetch Attendance Records")
        }

        if (isLoading) {
            CircularProgressIndicator()
        }

        // ✅ Export to PDF button
        if (studentList.isNotEmpty()) {
            Button(
                onClick = {
                    generatePdf(context, department, subject, studentList)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Export to PDF")
            }
        }

        // ✅ Scrollable Attendance List
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(studentList) { student ->
                val percentage = student.getInt("percentage")
                val cardColor = if (percentage < 75) Color(0xFFFFCDD2) else Color(0xFFC8E6C9)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Name: ${student.getString("name")}")
                        Text("ID: ${student.getString("studentId")}")
                        Text("Present: ${student.getInt("present")} / ${student.getInt("total")}")
                        Text("Percentage: $percentage%")
                    }
                }
            }
        }
    }
}
fun generatePdf(context: Context, department: String, subject: String, studentList: List<JSONObject>) {
    val pdfDocument = PdfDocument()
    val titlePaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 18f
        color = android.graphics.Color.BLACK
        isAntiAlias = true
    }
    val headerPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 12f
        isAntiAlias = true
    }
    val normalPaint = Paint().apply {
        textSize = 12f
        isAntiAlias = true
    }
    val borderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = android.graphics.Color.BLACK
    }

    val pageWidth = 595  // A4 at 72dpi
    val pageHeight = 842
    val margin = 40f
    val rowHeight = 25f
    val colNameWidth = 200f
    val colRollWidth = 100f
    val colPresentWidth = 100f
    val colPercentWidth = 80f
    val tableStartX = margin
    val textPadding = 5f

    // Partition students into three groups
    val highEnough = studentList.filter { it.optInt("percentage", 0) >= 75 }
    val belowThreshold = studentList.filter {
        val p = it.optInt("percentage", 0)
        p in 1..74
    }
    val zeroPercent = studentList.filter { it.optInt("percentage", 0) == 0 }

    var pageNumber = 1
    var currentPage = pdfDocument.startPage(
        PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    )
    var canvas = currentPage.canvas
    var y = margin

    // Helper to draw table header
    fun drawTableHeader(c: Canvas, yPos: Float) {
        c.drawRect(tableStartX, yPos, tableStartX + colNameWidth, yPos + rowHeight, borderPaint)
        c.drawRect(tableStartX + colNameWidth, yPos, tableStartX + colNameWidth + colRollWidth, yPos + rowHeight, borderPaint)
        c.drawRect(
            tableStartX + colNameWidth + colRollWidth,
            yPos,
            tableStartX + colNameWidth + colRollWidth + colPresentWidth,
            yPos + rowHeight,
            borderPaint
        )
        c.drawRect(
            tableStartX + colNameWidth + colRollWidth + colPresentWidth,
            yPos,
            tableStartX + colNameWidth + colRollWidth + colPresentWidth + colPercentWidth,
            yPos + rowHeight,
            borderPaint
        )
        c.drawText("Name", tableStartX + textPadding, yPos + 17f, headerPaint)
        c.drawText("Roll No.", tableStartX + colNameWidth + textPadding, yPos + 17f, headerPaint)
        c.drawText("Present", tableStartX + colNameWidth + colRollWidth + textPadding, yPos + 17f, headerPaint)
        c.drawText(
            "Percentage",
            tableStartX + colNameWidth + colRollWidth + colPresentWidth + textPadding,
            yPos + 17f,
            headerPaint
        )
    }

    // Utility to start new page and reset cursor
    fun newPage(resetTitle: Boolean = false, sectionTitle: String? = null) {
        pdfDocument.finishPage(currentPage)
        pageNumber += 1
        currentPage = pdfDocument.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        )
        canvas = currentPage.canvas
        y = margin
        if (resetTitle) {
            // if you want to repeat the main header on new pages, uncomment:
            // canvas.drawText("Attendance Summary", margin, y + 5f, titlePaint)
            // val dateStr = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
            // val dateText = "Exported on: $dateStr"
            // val dateWidth = normalPaint.measureText(dateText)
            // canvas.drawText(dateText, pageWidth - margin - dateWidth, y + 5f, normalPaint)
            // y += 30f
        }
        sectionTitle?.let {
            canvas.drawText(it, tableStartX, y, headerPaint)
            y += 20f
        }
    }

    // Title + metadata (only on first page)
    canvas.drawText("Attendance Summary", margin, y + 5f, titlePaint)
    val dateStr = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
    val dateText = "Exported on: $dateStr"
    val dateWidth = normalPaint.measureText(dateText)
    canvas.drawText(dateText, pageWidth - margin - dateWidth, y + 5f, normalPaint)
    y += 30f
    canvas.drawText("Department: $department", margin, y, headerPaint)
    y += 18f
    canvas.drawText("Subject: $subject", margin, y, headerPaint)
    y += 25f

    // Section: >=75%
    if (highEnough.isNotEmpty()) {
        canvas.drawText("Students with ≥75% Attendance", tableStartX, y, headerPaint)
        y += 15f
        drawTableHeader(canvas, y)
        y += rowHeight

        for (student in highEnough) {
            if (y + rowHeight + margin > pageHeight) {
                newPage(sectionTitle = "Students with ≥75% Attendance")
                drawTableHeader(canvas, y)
                y += rowHeight
            }

            // Row borders
            canvas.drawRect(tableStartX, y, tableStartX + colNameWidth, y + rowHeight, borderPaint)
            canvas.drawRect(tableStartX + colNameWidth, y, tableStartX + colNameWidth + colRollWidth, y + rowHeight, borderPaint)
            canvas.drawRect(
                tableStartX + colNameWidth + colRollWidth,
                y,
                tableStartX + colNameWidth + colRollWidth + colPresentWidth,
                y + rowHeight,
                borderPaint
            )
            canvas.drawRect(
                tableStartX + colNameWidth + colRollWidth + colPresentWidth,
                y,
                tableStartX + colNameWidth + colRollWidth + colPresentWidth + colPercentWidth,
                y + rowHeight,
                borderPaint
            )

            // Fill data
            val name = student.optString("name", "")
            val roll = student.optString("studentId", "")
            val present = "${student.optInt("present", 0)}/${student.optInt("total", 0)}"
            val percentage = "${student.optInt("percentage", 0)}%"

            canvas.drawText(name, tableStartX + textPadding, y + 17f, normalPaint)
            canvas.drawText(roll, tableStartX + colNameWidth + textPadding, y + 17f, normalPaint)
            canvas.drawText(present, tableStartX + colNameWidth + colRollWidth + textPadding, y + 17f, normalPaint)
            canvas.drawText(
                percentage,
                tableStartX + colNameWidth + colRollWidth + colPresentWidth + textPadding,
                y + 17f,
                normalPaint
            )

            y += rowHeight
        }
        y += 25f
    }

    // Section: below 75% but >0%
    if (belowThreshold.isNotEmpty()) {
        if (y + 60f > pageHeight) { // ensure space for heading+header
            newPage(sectionTitle = "Students with <75% Attendance")
        } else {
            canvas.drawText("Students with <75% Attendance", tableStartX, y, headerPaint)
            y += 15f
        }
        drawTableHeader(canvas, y)
        y += rowHeight

        for (student in belowThreshold) {
            if (y + rowHeight + margin > pageHeight) {
                newPage(sectionTitle = "Students with <75% Attendance")
                drawTableHeader(canvas, y)
                y += rowHeight
            }

            // Row borders
            canvas.drawRect(tableStartX, y, tableStartX + colNameWidth, y + rowHeight, borderPaint)
            canvas.drawRect(tableStartX + colNameWidth, y, tableStartX + colNameWidth + colRollWidth, y + rowHeight, borderPaint)
            canvas.drawRect(
                tableStartX + colNameWidth + colRollWidth,
                y,
                tableStartX + colNameWidth + colRollWidth + colPresentWidth,
                y + rowHeight,
                borderPaint
            )
            canvas.drawRect(
                tableStartX + colNameWidth + colRollWidth + colPresentWidth,
                y,
                tableStartX + colNameWidth + colRollWidth + colPresentWidth + colPercentWidth,
                y + rowHeight,
                borderPaint
            )

            // Fill data
            val name = student.optString("name", "")
            val roll = student.optString("studentId", "")
            val present = "${student.optInt("present", 0)}/${student.optInt("total", 0)}"
            val percentage = "${student.optInt("percentage", 0)}%"

            canvas.drawText(name, tableStartX + textPadding, y + 17f, normalPaint)
            canvas.drawText(roll, tableStartX + colNameWidth + textPadding, y + 17f, normalPaint)
            canvas.drawText(present, tableStartX + colNameWidth + colRollWidth + textPadding, y + 17f, normalPaint)
            canvas.drawText(
                percentage,
                tableStartX + colNameWidth + colRollWidth + colPresentWidth + textPadding,
                y + 17f,
                normalPaint
            )

            y += rowHeight
        }
        y += 25f
    }

    // Section: 0%
    if (zeroPercent.isNotEmpty()) {
        if (y + 60f > pageHeight) {
            newPage(sectionTitle = "Students with 0% Attendance")
        } else {
            canvas.drawText("Students with 0% Attendance", tableStartX, y, headerPaint)
            y += 15f
        }
        drawTableHeader(canvas, y)
        y += rowHeight

        for (student in zeroPercent) {
            if (y + rowHeight + margin > pageHeight) {
                newPage(sectionTitle = "Students with 0% Attendance")
                drawTableHeader(canvas, y)
                y += rowHeight
            }

            // Row borders
            canvas.drawRect(tableStartX, y, tableStartX + colNameWidth, y + rowHeight, borderPaint)
            canvas.drawRect(tableStartX + colNameWidth, y, tableStartX + colNameWidth + colRollWidth, y + rowHeight, borderPaint)
            canvas.drawRect(
                tableStartX + colNameWidth + colRollWidth,
                y,
                tableStartX + colNameWidth + colRollWidth + colPresentWidth,
                y + rowHeight,
                borderPaint
            )
            canvas.drawRect(
                tableStartX + colNameWidth + colRollWidth + colPresentWidth,
                y,
                tableStartX + colNameWidth + colRollWidth + colPresentWidth + colPercentWidth,
                y + rowHeight,
                borderPaint
            )

            // Fill data
            val name = student.optString("name", "")
            val roll = student.optString("studentId", "")
            val present = "${student.optInt("present", 0)}/${student.optInt("total", 0)}"
            val percentage = "0%"

            canvas.drawText(name, tableStartX + textPadding, y + 17f, normalPaint)
            canvas.drawText(roll, tableStartX + colNameWidth + textPadding, y + 17f, normalPaint)
            canvas.drawText(present, tableStartX + colNameWidth + colRollWidth + textPadding, y + 17f, normalPaint)
            canvas.drawText(
                percentage,
                tableStartX + colNameWidth + colRollWidth + colPresentWidth + textPadding,
                y + 17f,
                normalPaint
            )

            y += rowHeight
        }
    }

    pdfDocument.finishPage(currentPage)

    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
    val file = File(dir, "AttendanceSummary_${System.currentTimeMillis()}.pdf")

    try {
        pdfDocument.writeTo(FileOutputStream(file))
        Toast.makeText(context, "PDF saved to: ${file.absolutePath}", Toast.LENGTH_LONG).show()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
    } finally {
        pdfDocument.close()
    }
}
