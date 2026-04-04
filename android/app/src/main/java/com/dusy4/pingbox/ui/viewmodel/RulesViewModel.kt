package com.dusy4.pingbox.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dusy4.pingbox.data.local.RuleEntity
import com.dusy4.pingbox.data.local.TriggerDatabase
import com.dusy4.pingbox.data.preferences.AppPreferences
import com.dusy4.pingbox.data.remote.RuleCreateRequest
import com.dusy4.pingbox.data.remote.RuleResponse
import com.dusy4.pingbox.data.remote.RuleUpdateRequest
import com.dusy4.pingbox.data.remote.TriggerApiService
import com.dusy4.pingbox.util.UserFriendlyError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

data class RulesUiState(
    val rules: List<RuleEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val error: String? = null,
    val syncStatus: SyncStatus = SyncStatus.Idle
)

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Success(val message: String) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

class RulesViewModel(
    private val database: TriggerDatabase,
    private val preferences: AppPreferences,
    private val apiService: TriggerApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(RulesUiState())
    val uiState: StateFlow<RulesUiState> = _uiState.asStateFlow()

    init {
        loadRules()
    }

    fun loadRules() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val rules = database.ruleDao().getAll()
                _uiState.value = _uiState.value.copy(rules = rules, isLoading = false)
            } catch (e: Exception) {
                Timber.e(e, "Failed to load rules")
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun syncRulesFromServer() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncStatus = SyncStatus.Syncing)
            try {
                val token = preferences.accessToken.first()
                if (token.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncStatus = SyncStatus.Error("Not logged in")
                    )
                    return@launch
                }

                val response = apiService.getRules("Bearer $token")
                if (response.isSuccessful) {
                    val remoteRules = response.body()?.rules ?: emptyList()
                    remoteRules.forEach { remoteRule ->
                        database.ruleDao().insert(
                            RuleEntity(
                                id = remoteRule.id,
                                name = remoteRule.name,
                                matchTag = remoteRule.matchTag,
                                targetType = remoteRule.targetType,
                                targetValue = remoteRule.targetValue,
                                priority = remoteRule.priority,
                                enabled = remoteRule.enabled,
                                updatedAt = remoteRule.updatedAt
                            )
                        )
                    }
                    val rules = database.ruleDao().getAll()
                    _uiState.value = _uiState.value.copy(
                        rules = rules,
                        isSyncing = false,
                        syncStatus = SyncStatus.Success("Synced ${remoteRules.size} rules")
                    )
                    Timber.i("Synced ${remoteRules.size} rules from server")
                } else {
                    val errBody = response.errorBody()?.string()
                    val userMsg = UserFriendlyError.fromHttpError(response.code(), errBody)
                    Timber.w("Sync failed: ${response.code()} - $errBody")
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncStatus = SyncStatus.Error(userMsg)
                    )
                }
            } catch (e: Exception) {
                val userMsg = UserFriendlyError.fromException(e)
                Timber.e(e, "Sync failed")
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncStatus = SyncStatus.Error(userMsg)
                )
            }
        }
    }

    fun addRule(rule: RuleCreateRequest, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val token = preferences.accessToken.first()
                if (!token.isNullOrEmpty()) {
                    val response = apiService.createRule("Bearer $token", rule)
                    if (response.isSuccessful) {
                        val remoteRule = response.body()
                        if (remoteRule != null) {
                            database.ruleDao().insert(
                                RuleEntity(
                                    id = remoteRule.id,
                                    name = remoteRule.name,
                                    matchTag = remoteRule.matchTag,
                                    targetType = remoteRule.targetType,
                                    targetValue = remoteRule.targetValue,
                                    priority = remoteRule.priority,
                                    enabled = remoteRule.enabled,
                                    updatedAt = remoteRule.updatedAt
                                )
                            )
                        }
                    } else {
                        val errBody = response.errorBody()?.string()
                        Timber.w("Create rule failed: ${response.code()} - $errBody")
                    }
                }
                val rules = database.ruleDao().getAll()
                _uiState.value = _uiState.value.copy(rules = rules)
                onResult(true)
            } catch (e: Exception) {
                Timber.e(e, "Failed to add rule")
                onResult(false)
            }
        }
    }

    fun deleteRule(rule: RuleEntity) {
        viewModelScope.launch {
            try {
                val token = preferences.accessToken.first()
                if (!token.isNullOrEmpty()) {
                    val response = apiService.deleteRule("Bearer $token", rule.id)
                    if (response.isSuccessful) {
                        database.ruleDao().delete(rule)
                    } else {
                        Timber.w("Server delete failed, keeping local rule: ${response.code()}")
                    }
                } else {
                    database.ruleDao().delete(rule)
                }
                val rules = database.ruleDao().getAll()
                _uiState.value = _uiState.value.copy(rules = rules)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete rule")
            }
        }
    }

    fun updateRule(rule: RuleEntity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val token = preferences.accessToken.first()
                if (!token.isNullOrEmpty()) {
                    val updateRequest = RuleUpdateRequest(
                        name = rule.name,
                        matchTag = rule.matchTag,
                        targetType = rule.targetType,
                        targetValue = rule.targetValue,
                        priority = rule.priority,
                        enabled = rule.enabled
                    )
                    val response = apiService.updateRule("Bearer $token", rule.id, updateRequest)
                    if (response.isSuccessful) {
                        database.ruleDao().update(rule)
                    } else {
                        val errBody = response.errorBody()?.string()
                        Timber.w("Update rule failed: ${response.code()} - $errBody")
                        onResult(false)
                        return@launch
                    }
                } else {
                    database.ruleDao().update(rule)
                }
                val rules = database.ruleDao().getAll()
                _uiState.value = _uiState.value.copy(rules = rules)
                onResult(true)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update rule")
                onResult(false)
            }
        }
    }

    fun clearSyncStatus() {
        _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.Idle)
    }
}
