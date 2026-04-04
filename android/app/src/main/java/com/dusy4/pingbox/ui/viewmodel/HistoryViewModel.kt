package com.dusy4.pingbox.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dusy4.pingbox.data.local.NotificationEntity
import com.dusy4.pingbox.data.local.TriggerDatabase
import com.dusy4.pingbox.data.preferences.AppPreferences
import com.dusy4.pingbox.data.remote.TriggerApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

data class HistoryUiState(
    val notifications: List<NotificationEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class HistoryViewModel(
    private val database: TriggerDatabase,
    private val preferences: AppPreferences,
    private val apiService: TriggerApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val notifications = database.notificationDao().getAll()
                _uiState.value = _uiState.value.copy(
                    notifications = notifications,
                    isLoading = false
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to load notifications")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun syncHistoryFromServer() {
        viewModelScope.launch {
            try {
                val token = preferences.accessToken.first()
                if (token.isNullOrEmpty()) return@launch

                val response = apiService.getHistory("Bearer $token")
                if (response.isSuccessful) {
                    val remoteNotifications = response.body()?.notifications ?: emptyList()
                    remoteNotifications.forEach { remote ->
                        database.notificationDao().insert(
                            NotificationEntity(
                                id = remote.id,
                                title = remote.title,
                                body = remote.body,
                                icon = null,
                                tag = remote.tag,
                                targetType = remote.targetType,
                                targetValue = remote.targetValue
                            )
                        )
                    }
                    val notifications = database.notificationDao().getAll()
                    _uiState.value = _uiState.value.copy(notifications = notifications)
                    Timber.i("Synced ${remoteNotifications.size} notifications from server")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync history")
            }
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            try {
                database.notificationDao().delete(id)
                val notifications = database.notificationDao().getAll()
                _uiState.value = _uiState.value.copy(notifications = notifications)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete notification")
            }
        }
    }
}
