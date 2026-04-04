package com.dusy4.pingbox.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dusy4.pingbox.data.preferences.AppPreferences
import com.dusy4.pingbox.data.remote.LoginRequest
import com.dusy4.pingbox.data.remote.RegisterRequest
import com.dusy4.pingbox.data.remote.TriggerApiService
import com.dusy4.pingbox.util.UserFriendlyError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

data class SettingsUiState(
    val serverUrl: String = "https://your-server.example.com/",
    val apiKey: String = "",
    val deviceName: String = "",
    val accessToken: String = "",
    val userEmail: String = "",
    val isLoggedIn: Boolean = false,
    val hasOrphanedKey: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val snackbarMessage: String? = null
)

sealed class SettingsEvent {
    data class ShowSnackbar(val message: String) : SettingsEvent()
    object ApiKeyGenerated : SettingsEvent()
    object LoggedIn : SettingsEvent()
    object LoggedOut : SettingsEvent()
    object OrphanedKeyCleared : SettingsEvent()
}

class SettingsViewModel(
    private val preferences: AppPreferences,
    private val apiService: TriggerApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.serverUrl.collect { v -> _uiState.value = _uiState.value.copy(serverUrl = v ?: "https://your-server.example.com/") }
        }
        viewModelScope.launch {
            preferences.apiKey.collect { v ->
                val key = v ?: ""
                _uiState.value = _uiState.value.copy(apiKey = key)
                checkOrphanedKey(key)
            }
        }
        viewModelScope.launch {
            preferences.deviceName.collect { v -> _uiState.value = _uiState.value.copy(deviceName = v ?: "") }
        }
        viewModelScope.launch {
            preferences.accessToken.collect { v ->
                val token = v ?: ""
                _uiState.value = _uiState.value.copy(accessToken = token, isLoggedIn = token.isNotEmpty())
            }
        }
        viewModelScope.launch {
            preferences.userEmail.collect { v -> _uiState.value = _uiState.value.copy(userEmail = v ?: "") }
        }
    }

    /**
     * Check if there's an API key without a logged-in user (orphaned key from previous session).
     */
    private suspend fun checkOrphanedKey(apiKey: String) {
        val token = preferences.accessToken.first()
        if (apiKey.isNotEmpty() && token.isNullOrEmpty()) {
            _uiState.value = _uiState.value.copy(hasOrphanedKey = true)
        }
    }

    /**
     * Clear orphaned API key from previous session.
     */
    fun clearOrphanedKey() {
        viewModelScope.launch {
            preferences.setApiKey("")
            _uiState.value = _uiState.value.copy(hasOrphanedKey = false)
        }
    }

    fun login(email: String, password: String, onEvent: (SettingsEvent) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                Timber.i("Attempting login for $email")
                val response = apiService.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val token = response.body()?.accessToken
                    if (token.isNullOrEmpty()) {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = "No token received from server")
                        onEvent(SettingsEvent.ShowSnackbar("Server error: No token received"))
                    } else {
                        preferences.setAccessToken(token)
                        preferences.setUserEmail(email)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            hasOrphanedKey = false,
                            accessToken = token,
                            isLoggedIn = true,
                            userEmail = email
                        )
                        onEvent(SettingsEvent.LoggedIn)
                    }
                } else {
                    val errBody = response.errorBody()?.string()
                    val userMsg = UserFriendlyError.fromHttpError(response.code(), errBody)
                    Timber.w("Login failed: ${response.code()} - $errBody")
                    _uiState.value = _uiState.value.copy(isLoading = false, error = userMsg)
                    onEvent(SettingsEvent.ShowSnackbar(userMsg))
                }
            } catch (e: Exception) {
                val userMsg = UserFriendlyError.fromException(e)
                Timber.e(e, "Login failed")
                _uiState.value = _uiState.value.copy(isLoading = false, error = userMsg)
                onEvent(SettingsEvent.ShowSnackbar(userMsg))
            }
        }
    }

    fun register(email: String, password: String, onEvent: (SettingsEvent) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                Timber.i("Attempting register for $email")
                val response = apiService.register(RegisterRequest(email, password))
                if (response.isSuccessful) {
                    val token = response.body()?.accessToken
                    if (token.isNullOrEmpty()) {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = "No token received from server")
                        onEvent(SettingsEvent.ShowSnackbar("Server error: No token received"))
                    } else {
                        preferences.setAccessToken(token)
                        preferences.setUserEmail(email)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            hasOrphanedKey = false,
                            accessToken = token,
                            isLoggedIn = true,
                            userEmail = email
                        )
                        onEvent(SettingsEvent.LoggedIn)
                    }
                } else {
                    val errBody = response.errorBody()?.string()
                    val userMsg = UserFriendlyError.fromHttpError(response.code(), errBody)
                    Timber.w("Register failed: ${response.code()} - $errBody")
                    _uiState.value = _uiState.value.copy(isLoading = false, error = userMsg)
                    onEvent(SettingsEvent.ShowSnackbar(userMsg))
                }
            } catch (e: Exception) {
                val userMsg = UserFriendlyError.fromException(e)
                Timber.e(e, "Register failed")
                _uiState.value = _uiState.value.copy(isLoading = false, error = userMsg)
                onEvent(SettingsEvent.ShowSnackbar(userMsg))
            }
        }
    }

    fun generateApiKey(onEvent: (SettingsEvent) -> Unit) {
        viewModelScope.launch {
            val token = _uiState.value.accessToken
            if (token.isEmpty()) {
                onEvent(SettingsEvent.ShowSnackbar("Log in first, then generate an API key"))
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = apiService.createApiKey("Bearer $token")
                if (response.isSuccessful) {
                    val newKey = response.body()?.key ?: ""
                    if (newKey.isNotEmpty()) {
                        preferences.setApiKey(newKey)
                        _uiState.value = _uiState.value.copy(isLoading = false, hasOrphanedKey = false)
                        onEvent(SettingsEvent.ApiKeyGenerated)
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = "Server returned empty key")
                        onEvent(SettingsEvent.ShowSnackbar("Server error: Empty key received"))
                    }
                } else {
                    val errBody = response.errorBody()?.string()
                    val userMsg = UserFriendlyError.fromHttpError(response.code(), errBody)
                    Timber.w("API key generation failed: ${response.code()} - $errBody")
                    _uiState.value = _uiState.value.copy(isLoading = false, error = userMsg)
                    onEvent(SettingsEvent.ShowSnackbar("Failed to generate key: $userMsg"))
                }
            } catch (e: Exception) {
                val userMsg = UserFriendlyError.fromException(e)
                Timber.e(e, "API key generation failed")
                _uiState.value = _uiState.value.copy(isLoading = false, error = userMsg)
                onEvent(SettingsEvent.ShowSnackbar("Failed to generate key: $userMsg"))
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            // Clear both JWT token AND API key (key belongs to that account)
            preferences.logout()
            preferences.setApiKey("")
            _uiState.value = _uiState.value.copy(hasOrphanedKey = false)
            Timber.i("Logged out, cleared API key")
        }
    }

    fun saveConfiguration(deviceName: String, onEvent: (SettingsEvent) -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            when {
                !state.isLoggedIn && state.apiKey.isEmpty() -> {
                    onEvent(SettingsEvent.ShowSnackbar("Log in first, then generate an API key"))
                    return@launch
                }
                state.isLoggedIn && state.apiKey.isEmpty() -> {
                    onEvent(SettingsEvent.ShowSnackbar("Tap 'Generate New Key' above first"))
                    return@launch
                }
                !state.isLoggedIn && state.apiKey.isNotEmpty() -> {
                    onEvent(SettingsEvent.ShowSnackbar("API key is from a previous session. Log in and generate a new key."))
                    return@launch
                }
            }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                preferences.setDeviceName(deviceName)
                onEvent(SettingsEvent.ShowSnackbar("Configuration saved"))
            } catch (e: Exception) {
                onEvent(SettingsEvent.ShowSnackbar("Failed to save: ${e.message}"))
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSnackbarMessage() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
