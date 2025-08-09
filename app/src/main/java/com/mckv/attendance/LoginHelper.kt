package com.mckv.attendance

import android.content.Context
import android.widget.Toast
import androidx.navigation.NavController
import com.mckv.attendance.model.LoginRequest
import com.mckv.attendance.model.RetrofitClient
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

fun loginUser(
    request: LoginRequest,
    context: Context,
    navController: NavController
) {
    val call = RetrofitClient.instance.loginUser(request)

    call.enqueue(object : Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            System.out.println("🟢 Sent: $request")
            System.out.println("🟢 Response Code: ${response.code()}")

            if (response.isSuccessful) {
                val bodyString = response.body()?.string()
                System.out.println("✅ Raw Response Body: $bodyString")

                if (bodyString != null) {
                    try {
                        val json = JSONObject(bodyString)
                        val success = json.optBoolean("success")
                        val message = json.optString("message")
                        val role = json.optString("role") // 👈 extract role
                        val id = json.optString("id") // ✅ Extract ID
                        val department = json.optString("department")

                        if (success) {
                            //SessionManager.teacherId = id // ✅ Store ID globally
                            // ✅ Save to SessionManager based on role
                            when (role) {
                                "ROLE_STUDENT" -> {
                                    SessionManager.studentId = id
                                    SessionManager.department = department
                                    System.out.println("💾 Stored studentId: $id")
                                    System.out.println("💾 Stored department: $department")
                                }

                                "ROLE_TEACHER" -> {
                                    SessionManager.teacherId = id
                                    System.out.println("💾 Stored teacherId: $id")
                                }
                            }
                            Toast.makeText(context, "✅ $message", Toast.LENGTH_LONG).show()
                            //navController.navigate("home_screen")
                            // 👇 Navigate based on role
                            when (role) {
                                "ROLE_STUDENT" -> navController.navigate("home")
                                "ROLE_TEACHER" -> navController.navigate("teacher")
                                "ROLE_ADMIN" -> navController.navigate("admin_dashboard")
                                "ROLE_CENTER" -> navController.navigate("center_dashboard")
                                else -> Toast.makeText(context, "⚠ Unknown role: $role", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "❌ $message", Toast.LENGTH_LONG).show()
                        }

                    } catch (e: Exception) {
                        System.out.println("❌ JSON Parsing Error: ${e.message}")
                        Toast.makeText(context, "⚠️ Invalid server response", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "⚠️ Empty response body", Toast.LENGTH_LONG).show()
                }

            } else {
                val errorBody = response.errorBody()?.string()
                System.out.println("❌ Error Body: $errorBody")
                Toast.makeText(context, "⚠️ Server Error: $errorBody", Toast.LENGTH_LONG).show()
            }
        }

        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            System.out.println("🚫 Failure: ${t.message}")
            Toast.makeText(context, "🚫 Network error: ${t.message}", Toast.LENGTH_LONG).show()
        }
    })
}
