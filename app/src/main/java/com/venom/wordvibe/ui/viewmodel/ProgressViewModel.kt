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
import java.util.Calendar
import javax.inject.Inject

/**
 * Phase 1 progress tab: streak, daily goal ring, word-state classification, and
 * activity totals — plus a read-only 7-day review forecast from the existing
 * nextReview column. No contract surface.
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
        val reviewForecast: List<Int> = emptyList(),
        val scheduledToday: Int = 0,
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
                val now = System.currentTimeMillis()
                val scheduledTimes = progressRepository.getScheduledReviewTimes(
                    userId,
                    startOfLocalDay(now),
                )
                val forecast = buildSevenDayForecast(scheduledTimes, now)
                _state.update {
                    it.copy(
                        isLoading = false,
                        dashboard = activityRepository.getDashboardData(userId),
                        newWordsCount = progressRepository.countNewWordsForUser(userId),
                        knownWordsCount = progressRepository.countKnownWordsForUser(userId),
                        reviewForecast = forecast,
                        scheduledToday = forecast.firstOrNull() ?: 0,
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

/* ───── 7-day review forecast bucketing (pure, unit-testable) ───── */

/** Local calendar midnight of the day containing [timeMillis]. */
private fun startOfLocalDay(timeMillis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timeMillis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/**
 * Buckets scheduled review timestamps into 7 local calendar days starting today
 * (index 0 = today). Timestamps before today or past +6 days are ignored.
 * Day boundaries are computed per-day via Calendar, so DST shifts stay correct.
 */
internal fun buildSevenDayForecast(scheduledTimes: List<Long>, now: Long): List<Int> {
    val startOfToday = startOfLocalDay(now)
    val buckets = IntArray(7)
    // Wall-clock day boundaries via Calendar.add — fixed 24h math drifts across DST shifts.
    val boundaries = List(8) { d ->
        val cal = Calendar.getInstance()
        cal.timeInMillis = startOfToday
        cal.add(Calendar.DAY_OF_YEAR, d)
        cal.timeInMillis
    }
    scheduledTimes.forEach { t ->
        for (day in 0 until 7) {
            if (t >= boundaries[day] && t < boundaries[day + 1]) {
                buckets[day] += 1
                break
            }
        }
    }
    return buckets.toList()
}
