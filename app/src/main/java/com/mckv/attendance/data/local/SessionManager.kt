package com.mckv.attendance.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.mckv.attendance.data.local.PermissionManager.clearPermissions
import com.mckv.attendance.data.model.UserDetails
import com.mckv.attendance.utils.JwtUtils

object SessionManager {
    //SHARED PREFERENCES
    private lateinit var prefs: SharedPreferences
    //GSON
    private val gson= Gson()

    private const val PREF_NAME= "proxino_session_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_JSON = "user_details_json"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    //INITIALIZE OF SHARED PREFERENCES AS LATE INIT, MODE_PRIVATE ENABLE ONLY THIS APP TO USE THE STORAGE NAMED proxino_prefs
    //PREVENTING OTHER APPS TO USE THIS STORAGE
    fun init(context: Context){
        prefs= context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    //AUTHENTICATION SECTION
    var authToken: String?
        get() = prefs.getString(KEY_TOKEN, null)

        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    //CHECK USER LOGIN STATUS
    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false) && !isTokenExpired()

        private set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var userDetails: UserDetails?
        get() {
            val json= prefs.getString(KEY_USER_JSON, null)
            return if (json!= null) gson.fromJson(json, UserDetails::class.java) else null
        }

        set(value){
            val json= gson.toJson(value)
            prefs.edit().putString(KEY_USER_JSON, json).apply()
        }

    //CORE
    fun saveSession(token: String, user: UserDetails){
        this.authToken= token
        this.userDetails= user
        this.isLoggedIn= true
    }

    //SECURITY CHEC
    fun isTokenExpired(): Boolean{
        val token= authToken ?: return true
        return JwtUtils.isTokenExpired(token)
    }

    //CLEAR SHARED PREFERENCES
    fun logout(){
        prefs.edit().clear().apply()
        clearPermissions()
    }
}