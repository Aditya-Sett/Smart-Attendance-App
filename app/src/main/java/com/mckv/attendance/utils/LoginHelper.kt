package com.mckv.attendance.utils

import android.content.Context
import android.widget.Toast
import androidx.navigation.NavController
import com.google.gson.Gson
import com.mckv.attendance.data.local.TokenExpiryManager
import com.mckv.attendance.data.remote.dto.request.LoginRequest
import com.mckv.attendance.data.remote.RetrofitClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.mckv.attendance.data.local.PermissionManager
import com.mckv.attendance.data.local.SessionManager
import com.mckv.attendance.data.model.UserDetails
import com.mckv.attendance.data.remote.dto.response.LoginResponse
import com.mckv.attendance.data.remote.dto.response.ProfileResponse
import com.mckv.attendance.data.remote.dto.response.RolePermissionsResponse

private val gson = Gson()

fun loginUser(
    request: LoginRequest,
    context: Context,
    navController: NavController,
    onComplete: () -> Unit = {}
) {
    RetrofitClient.authInstance.loginUser(request)
        .enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val bodyString = response.body()?.string()
                    if (!bodyString.isNullOrEmpty()) {
                        try {
                            val loginResponse = gson.fromJson(bodyString, LoginResponse::class.java)
                            if (loginResponse.success && loginResponse.data != null) {
                                val token = loginResponse.data.token
                                // ✅ Pass token forward; don't save session yet
                                fetchUserProfile(token, context, navController, onComplete)
                                return // onComplete() will be called deep in the chain
                            } else {
                                Toast.makeText(context, "❌ ${loginResponse.message}", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "❌ Parse error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "⚠️ Empty response body", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(context, "⚠️ Server Error: $errorBody", Toast.LENGTH_LONG).show()
                }
                onComplete() // ✅ Always called on any failure path here
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(context, "🚫 Network error: ${t.message}", Toast.LENGTH_LONG).show()
                onComplete() // ✅
            }
        })
}

private fun fetchUserProfile(
    token: String,
    context: Context,
    navController: NavController,
    onComplete: () -> Unit = {}
) {
    RetrofitClient.authInstance.getProfile("Bearer $token")
        .enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val bodyString = response.body()?.string()
                    if (!bodyString.isNullOrEmpty()) {
                        try {
                            val profileJson = gson.fromJson(bodyString, ProfileResponse::class.java)
                            if (profileJson.success && profileJson.data != null) {
                                // ✅ Do NOT save session yet — wait until all steps succeed
                                fetchPermissionsForRoles(
                                    token = token,
                                    profile = profileJson.data,
                                    userRoles = profileJson.data.role,
                                    context = context,
                                    navController = navController,
                                    onComplete = onComplete
                                )
                                return // onComplete() will be called by fetchPermissionsForRoles
                            } else {
                                Toast.makeText(context, "❌ Profile error: ${profileJson.success}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "❌ Profile parse error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "⚠️ Empty profile body", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "⚠️ Profile Server Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
                onComplete() // ✅ Always called on any failure path here
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(context, "🚫 Network error (profile)", Toast.LENGTH_LONG).show()
                onComplete() // ✅
            }
        })
}

private fun fetchPermissionsForRoles(
    token: String,
    profile: UserDetails, // use your actual type
    userRoles: List<String>?,
    context: Context,
    navController: NavController,
    onComplete: () -> Unit
) {
    if (userRoles.isNullOrEmpty()) {
        Toast.makeText(context, "⚠️ No roles found", Toast.LENGTH_SHORT).show()
        onComplete() // ✅ FIX: was `onComplete` (no parentheses) — never called before!
        return
    }

    RetrofitClient.rolePermissionInstance
        .getAllPermissionForRoles(userRoles, "android-secret")
        .enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                try {
                    if (response.isSuccessful) {
                        val bodyString = response.body()?.string()
                        if (!bodyString.isNullOrEmpty()) {
                            val roleResponse = gson.fromJson(bodyString, RolePermissionsResponse::class.java)
                            if (roleResponse.success && roleResponse.data != null) {
                                val permissionsList = roleResponse.data.map { it.permission }.distinct()
                                PermissionManager.setPermissions(permissionsList)

                                // ✅ Only save session HERE — after ALL steps succeed
                                SessionManager.saveSession(token, profile)

                                val roles = profile.role ?: emptyList()
                                val target = if (roles.contains("STUDENT")) "home" else "dynamic_dashboard"
                                navController.navigate(target) {
                                    popUpTo(0) { inclusive = true }
                                }
                            } else {
                                Toast.makeText(context, "❌ Permission fetch failed", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "⚠️ Empty permissions body", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // ✅ FIX: onComplete() was missing in this branch before!
                        Toast.makeText(context, "ERROR: PERMISSIONS", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "NETWORK ERROR", Toast.LENGTH_SHORT).show()
                } finally {
                    onComplete() // ✅ Always called regardless of success or failure
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(context, "NETWORK ERROR", Toast.LENGTH_LONG).show()
                onComplete() // ✅
            }
        })
}

fun logoutUser(context: Context, navController: NavController?) {
    TokenExpiryManager.setDialogShowing(false)
    SessionManager.logout()
    navController?.navigate("login_screen") {
        popUpTo(0) { inclusive = true }
    }
    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
}