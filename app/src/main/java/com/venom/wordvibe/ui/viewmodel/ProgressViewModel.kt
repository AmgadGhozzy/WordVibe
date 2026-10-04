package com.venom.wordvibe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venom.data.repo.UserActivityRepository
import com.venom.data.repo.UserWordProgressRepository
import com.venom.domain.model.DashboardData
import com.venom.domain.repo.IUserIdentityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) : ViewModel() {

    data class ProgressUiState(
        val isLoading: Boolean = true,
        val dashboard: DashboardData? = null,
        val newWordsCount: Int = 0,
        val knownWordsCount: Int = 0,
    )

    private val _state = MutableStateFlow(ProgressUiState())
    val state: StateFlow<ProgressUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                val userId = identityRepository.getCurrentUserId()
                _state.value = ProgressUiState(
                    isLoading = false,
                    dashboard = activityRepository.getDashboardData(userId),
                    newWordsCount = progressRepository.countNewWordsForUser(userId),
                    knownWordsCount = progressRepository.countKnownWordsForUser(userId),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value = ProgressUiState(isLoading = false)
            }
        }
    }
}
