/*
Manage saving, retrieving, and clearing credentials securely
*/
package com.mckv.attendance.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

object UserPreferences {
    private val KEY_REMEMBER_ME = booleanPreferencesKey("remember_me")
    private val KEY_USERNAME = stringPreferencesKey("saved_username")
    private val KEY_PASSWORD = stringPreferencesKey("saved_password")

    // Get saved credentials as a Flow
    fun getSavedCredentials(context: Context): Flow<Triple<Boolean, String, String>> {
        return context.dataStore.data.map { prefs ->
            Triple(
                prefs[KEY_REMEMBER_ME] ?: false,
                prefs[KEY_USERNAME] ?: "",
                prefs[KEY_PASSWORD] ?: ""
            )
        }
    }

    // Save or clear credentials based on checkbox state
    suspend fun saveCredentials(context: Context, remember: Boolean, username: String, password: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REMEMBER_ME] = remember
            if (remember) {
                prefs[KEY_USERNAME] = username
                prefs[KEY_PASSWORD] = password
            } else {
                prefs.remove(KEY_USERNAME)
                prefs.remove(KEY_PASSWORD)
            }
        }
    }
}