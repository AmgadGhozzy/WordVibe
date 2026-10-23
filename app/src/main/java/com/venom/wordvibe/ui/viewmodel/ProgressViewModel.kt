package com.venom.wordvibe.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venom.data.local.PreferencesKeys
import com.venom.data.repo.UserActivityRepository
import com.venom.data.repo.UserWordProgressRepository
import com.venom.domain.model.AppTheme
import com.venom.domain.model.DashboardData
import com.venom.domain.repo.IUserIdentityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Phase 1 progress tab: reads the existing SQL aggregates only (streak, word-state
 * classification, activity) — no new queries, no contract surface.
 */
@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val identityRepository: IUserIdentityRepository,
    private val activityRepository: UserActivityRepository,
    private val progressRepository: UserWordProgressRepository,
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {
    data class ProgressUiState(
        val isLoading: Boolean = true,
        val dashboard: DashboardData? = null,
        val newWordsCount: Int = 0,
        val knownWordsCount: Int = 0,
        val appTheme: AppTheme = AppTheme.SYSTEM,
        val autoPronounce: Boolean = true,
    )

    private val _state = MutableStateFlow(ProgressUiState())
    val state: StateFlow<ProgressUiState> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            dataStore.data.map { prefs ->
                prefs[PreferencesKeys.APP_THEME]?.let { raw ->
                    AppTheme.entries.find { it.name == raw }
                } ?: AppTheme.SYSTEM
            }.collect { theme ->
                _state.update { it.copy(appTheme = theme) }
            }
        }
        viewModelScope.launch {
            dataStore.data.map { it[PreferencesKeys.AUTO_PRONOUNCE] ?: true }.collect { enabled ->
                _state.update { it.copy(autoPronounce = enabled) }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            try {
                val userId = identityRepository.getCurrentUserId()
                _state.update {
                    it.copy(
                        isLoading = false,
                        dashboard = activityRepository.getDashboardData(userId),
                        newWordsCount = progressRepository.countNewWordsForUser(userId),
                        knownWordsCount = progressRepository.countKnownWordsForUser(userId),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateDailyGoal(goalXp: Int) {
        viewModelScope.launch {
            try {
                val userId = identityRepository.getCurrentUserId()
                activityRepository.updateDailyGoal(userId, goalXp)
                load()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    fun updateAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            dataStore.edit { it[PreferencesKeys.APP_THEME] = theme.name }
        }
    }

    fun updateAutoPronounce(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[PreferencesKeys.AUTO_PRONOUNCE] = enabled }
        }
    }
}
