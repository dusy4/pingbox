package com.dusy4.pingbox.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

object PreferencesKeys {
    val SERVER_URL = stringPreferencesKey("server_url")
    val API_KEY = stringPreferencesKey("api_key")
    val DEVICE_NAME = stringPreferencesKey("device_name")
    val ACCESS_TOKEN = stringPreferencesKey("access_token")
    val USER_EMAIL = stringPreferencesKey("user_email")
    val DEVICE_ID = stringPreferencesKey("device_id")
    val DEBUG_MODE = booleanPreferencesKey("debug_mode")
}

class AppPreferences(private val context: Context) {

    val serverUrl: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SERVER_URL] ?: "https://your-server.example.com/"
    }

    val apiKey: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.API_KEY]
    }

    val deviceName: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DEVICE_NAME] ?: android.os.Build.MODEL
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.ACCESS_TOKEN]
    }

    val userEmail: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.USER_EMAIL]
    }

    val deviceId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DEVICE_ID]
    }

    val debugMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DEBUG_MODE] ?: false
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[PreferencesKeys.ACCESS_TOKEN].isNullOrEmpty()
    }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SERVER_URL] = url
        }
    }

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.API_KEY] = key
        }
    }

    suspend fun setDeviceName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DEVICE_NAME] = name
        }
    }

    suspend fun setAccessToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.ACCESS_TOKEN] = token
        }
    }

    suspend fun setUserEmail(email: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.USER_EMAIL] = email
        }
    }

    suspend fun setDeviceId(id: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DEVICE_ID] = id
        }
    }

    suspend fun setDebugMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DEBUG_MODE] = enabled
        }
    }

    suspend fun logout() {
        context.dataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.ACCESS_TOKEN)
            prefs.remove(PreferencesKeys.USER_EMAIL)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
