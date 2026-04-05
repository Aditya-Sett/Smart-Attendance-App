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

    //CALL TO AUTH API LOGIN
    RetrofitClient.authInstance.loginUser(request)
        .enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                //IF RESPONSE IS SUCCESSFUL
                if (response.isSuccessful) {

                    //The ? says: "Only try to read the content if the body actually exists.
                    //If it's empty, just return null instead of crashing the app.
                    //By default, the data coming over the internet is just a stream of Bytes (0s and 1s).
                    //.string(): This converts those raw bytes into a readable UTF-8 String
                    val bodyString = response.body()?.string()

                    if (!bodyString.isNullOrEmpty()) {
                        try {
                            //CONVERT THE STRING TO KOTLIN OBJECT
                            val loginResponse = gson.fromJson(bodyString, LoginResponse::class.java)

                            if (loginResponse.success && loginResponse.data != null) {
                                //TOKEN
                                val token = loginResponse.data.token
                                //ROLE
                                val roles = loginResponse.data.role

                                // NOW FETCH PROFILE DATA
                                fetchUserProfile(token, context, navController, onComplete)

                                return
                            } else {
                                Toast.makeText(context, "❌ ${loginResponse.message}", Toast.LENGTH_LONG).show()
                            }

                        } catch (e: Exception) {
                            System.out.println("❌ JSON Parsing Error: ${e.message}")
                        }
                    } else {
                        Toast.makeText(context, "⚠️ Empty response body", Toast.LENGTH_LONG).show()
                    }

                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(context, "⚠️ Server Error: $errorBody", Toast.LENGTH_LONG).show()
                }
                onComplete()
            }


            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(context, "🚫 Network error: ${t.message}", Toast.LENGTH_LONG).show()

                onComplete()
            }
        })
}

// 👇 New function to fetch user profile
private fun fetchUserProfile(
    token: String,
    context: Context,
    navController: NavController,
    onComplete: () -> Unit = {}
) {

    val call = RetrofitClient.authInstance.getProfile("Bearer $token")

    call.enqueue(object : Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            try {
                // 1. Start TRY at the very beginning of the response
                if (response.isSuccessful) {
                    val bodyString = response.body()?.string()

                    if (!bodyString.isNullOrEmpty()) {
                        val profileJson = gson.fromJson(bodyString, ProfileResponse::class.java)

                        if (profileJson.success && profileJson.data != null) {
                            // SAVE SESSION
                            SessionManager.saveSession(token, profileJson.data)

                            // FETCH PERMISSION FOR ROLE
                            fetchPermissionsForRoles(profileJson.data.role, context, navController, onComplete)

                            return
                        }else{
                            Toast.makeText(context, "❌ ${profileJson.success}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "⚠️ Server Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                println("❌ Parsing Error: ${e.message}")
            }
            onComplete()
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
    SessionManager.logout()

    // Navigate to main home screen and clear back stack
    navController?.navigate("login_screen") {  // Make sure this matches your actual route
        popUpTo(0) { inclusive = true }
    }


    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
}

private fun fetchPermissionsForRoles(
    userRoles: List<String>?,
    context: Context,
    navController: NavController,
    onComplete: () -> Unit
) {

    if (userRoles.isNullOrEmpty()) {
        println("⚠ Roles are null, cannot fetch permissions")
        onComplete
        return
    }

    val call = RetrofitClient.rolePermissionInstance
        .getAllPermissionForRoles(userRoles, "android-secret")

    call.enqueue(object : Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {

            if (response.isSuccessful) {
                val bodyString = response.body()?.string()

                if (bodyString != null) {
                    try {

                        val roleResponse = gson.fromJson(bodyString, RolePermissionsResponse::class.java)

                        if (roleResponse.success && roleResponse.data != null) {
                            //EXTRACT PERMISSION FROM LIST AND RETURN DISTINCT LIST
                            val permissionsList = roleResponse.data.map { it.permission }.distinct()

                            // ✅ Save permissions
                            PermissionManager.setPermissions(permissionsList)

                            // ROLES & NAVIGATION
                            val roles = SessionManager.userDetails?.role ?: emptyList()
                            val target = if (roles.contains("STUDENT")) "home" else "dynamic_dashboard"

                            navController.navigate(target) {
                                popUpTo(0) { inclusive = true }
                            }

                        } else {
                            println("❌ $roleResponse")
                        }

                    } catch (e: Exception) {
                        println("❌ Permission: ${e.message}")
                    }finally {
                        onComplete()
                    }
                }

            } else {
                System.out.println("❌ Permission API Error: ${response.code()}")
            }
        }

        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            println("🚫 Permission API Failure: ${t.message}")

            onComplete()
        }
    })
}
