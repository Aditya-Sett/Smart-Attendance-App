package com.mckv.attendance.utils

import android.content.Context
import android.widget.Toast
import androidx.navigation.NavController
import com.mckv.attendance.data.local.SessionManager
import com.mckv.attendance.data.local.TokenExpiryManager
import com.mckv.attendance.data.remote.dto.request.LoginRequest
import com.mckv.attendance.data.remote.RetrofitClient
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

fun loginUser(
    request: LoginRequest,
    context: Context,
    navController: NavController,
    onComplete: () -> Unit = {}
) {
    val call = RetrofitClient.authInstance.loginUser(request)

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
//                        val role = json.optString("role") // 👈 extract role
//                        val id = json.optString("id") // ✅ Extract ID
//                        val department = json.optString("department")

                        var token=""
                        var id=""

                        val data=json.optJSONObject("data")
                        if(data!=null){
                            token=data.optString("token")
                            id=data.optString("id")
                        }



                        if (success && token.isNotEmpty()) {

                            // Save token to SessionManager (for future API calls)
                            SessionManager.authToken =token

                            // Now fetch profile data using the token
                            fetchUserProfile(token,id, context, navController, onComplete)

                            //SessionManager.teacherId = id // ✅ Store ID globally
                            // ✅ Save to SessionManager based on role
//                            when (role) {
//                                "ROLE_STUDENT" -> {
//                                    SessionManager.studentId = id
//                                    SessionManager.department = department
//                                    System.out.println("💾 Stored studentId: $id")
//                                    System.out.println("💾 Stored department: $department")
//                                }
//
//                                "ROLE_TEACHER" -> {
//                                    SessionManager.teacherId = id
//                                    System.out.println("💾 Stored teacherId: $id")
//                                }
//                            }

//                            Toast.makeText(context, "✅ $message", Toast.LENGTH_LONG).show()

                            //navController.navigate("home_screen")
                            // 👇 Navigate based on role
//                            when (role) {
//                                "ROLE_STUDENT" -> navController.navigate("home")
//                                "ROLE_TEACHER" -> navController.navigate("teacher")
//                                "ROLE_ADMIN" -> navController.navigate("admin_dashboard")
//                                "ROLE_CENTER" -> navController.navigate("center_dashboard")
//                                else -> Toast.makeText(context, "⚠ Unknown role: $role", Toast.LENGTH_LONG).show()
//                            }

                        } else {
                            Toast.makeText(context, "❌ $message", Toast.LENGTH_LONG).show()
                        }

                    } catch (e: Exception) {
                        System.out.println("❌ JSON Parsing Error: ${e.message}")
                        Toast.makeText(context, "⚠️ Invalid server response", Toast.LENGTH_LONG).show()
                    } finally {
                        onComplete() // ✅ stop loader
                    }
                } else {
                    Toast.makeText(context, "⚠️ Empty response body", Toast.LENGTH_LONG).show()
                }

            } else {
                val errorBody = response.errorBody()?.string()
                System.out.println("❌ Error Body: $errorBody")
                Toast.makeText(context, "⚠️ Server Error: $errorBody", Toast.LENGTH_LONG).show()
            }

            onComplete()
        }

        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            System.out.println("🚫 Failure: ${t.message}")
            Toast.makeText(context, "🚫 Network error: ${t.message}", Toast.LENGTH_LONG).show()
            onComplete()
        }
    })
}

// 👇 New function to fetch user profile
private fun fetchUserProfile(token: String,id: String, context: Context, navController: NavController, onComplete: () -> Unit = {}) {
    val call = RetrofitClient.authInstance.getProfile("Bearer $token")

    call.enqueue(object : Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            if (response.isSuccessful) {
                val bodyString = response.body()?.string()

                if (bodyString != null) {
                    try {
                        val json = JSONObject(bodyString)
                        val success = json.optBoolean("success")
                        val message = json.optString("message")

                        if (success) {
                            val profileData = json.optJSONObject("data")
                            System.out.println("🟩 Raw profileData: " + profileData.toString())
                            if (profileData != null) {
                                val role = profileData.optString("role")
                                val department = profileData.optString("department")
                                val studentId = profileData.optString("studentId")
                                val admissionYear = profileData.optString("admission_year",profileData.optString("admissionYear", ""))
                                System.out.println("🟩 Extracted admissionYear: '$admissionYear'")

                                // ✅ Save complete login session
                                SessionManager.saveLoginSession(token, role, studentId)

                                // ✅ Save profile data to SessionManager
                                when (role) {
                                    "ROLE_STUDENT" -> {


                                        SessionManager.studentId = studentId
                                        SessionManager.department = department
                                        SessionManager.admissionYear = admissionYear
                                    }
                                    "ROLE_TEACHER" -> {
                                        SessionManager.teacherId = studentId
                                        SessionManager.department= department
                                    }
                                    "ADMIN" -> {
                                        SessionManager.adminId = studentId
                                    }
                                    else -> {
                                        Toast.makeText(context, "⚠ Unknown role", Toast.LENGTH_LONG).show()
                                    }
                                }

                                System.out.println("💾 Login session saved - Role: $role, ID: $studentId")

                                // ✅ Navigate based on role
                                when (role) {
                                    "ROLE_STUDENT" -> navController.navigate("home") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                    "ROLE_TEACHER" -> navController.navigate("teacher") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                    "ADMIN" -> navController.navigate("admin_dashboard") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                    else -> Toast.makeText(context, "⚠ Unknown role", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "❌ $message", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "⚠️ Profile data error", Toast.LENGTH_LONG).show()
                    } finally {
                        onComplete() // ✅ always stop loader after response
                    }
                }
            } else {
                Toast.makeText(context, "⚠️ Failed to fetch profile", Toast.LENGTH_LONG).show()
            }
        }

        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            Toast.makeText(context, "🚫 Network error", Toast.LENGTH_LONG).show()
            onComplete()
        }
    })
}

// Add this to your utils package
fun logoutUser(context: Context, navController: NavController?) {

    // Clear token expiry state
    TokenExpiryManager.setDialogShowing(false)


    // Clear session data
    SessionManager.clear()

    // Navigate to main home screen and clear back stack
    navController?.navigate("main_home") {  // Make sure this matches your actual route
        popUpTo(0) { inclusive = true }
    }


    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
}
