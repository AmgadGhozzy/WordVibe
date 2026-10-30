package com.venom.wordvibe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venom.data.repo.UserActivityRepository
import com.venom.data.repo.UserWordProgressRepository
import com.venom.domain.repo.IUserIdentityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * app02 §02: read-only analytics time series for the Progress tab — review
 * attempts (quiz_attempts) and production successes (userActivity) bucketed
 * per local day over a 7/30-day window. Pure bucketing lives in
 * ProgressAnalyticsSeries.kt. Word-state-change history is a declared
 * LIMITATION (no state-event table; no schema changes allowed).
 */
@HiltViewModel
class ProgressAnalyticsViewModel @Inject constructor(
    private val identityRepository: IUserIdentityRepository,
    private val progressRepository: UserWordProgressRepository,
    private val activityRepository: UserActivityRepository,
) : ViewModel() {

    data class AnalyticsUiState(
        val windowDays: Int = 7,
        val reviewSeries: List<Int> = emptyList(),
        val productionSeries: List<Int> = emptyList(),
    )

    private val _state = MutableStateFlow(AnalyticsUiState())
    val state: StateFlow<AnalyticsUiState> = _state.asStateFlow()

    init {
        load(7)
    }

    fun load(windowDays: Int) {
        viewModelScope.launch {
            try {
                val userId = identityRepository.getCurrentUserId()
                val dateKeys = dateKeysForWindow(System.currentTimeMillis(), windowDays)
                val fromKey = dateKeys.first()
                val attemptCounts = progressRepository.getAttemptCountsByDate(userId, fromKey)
                    .associate { it.day to it.count }
                val productionCounts = activityRepository.getProductionCountsByDate(userId, fromKey)
                    .associate { it.day to it.count }
                _state.value = AnalyticsUiState(
                    windowDays = windowDays,
                    reviewSeries = buildSeries(dateKeys, attemptCounts),
                    productionSeries = buildSeries(dateKeys, productionCounts),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value = AnalyticsUiState(windowDays = windowDays)
            }
        }
    }
}
