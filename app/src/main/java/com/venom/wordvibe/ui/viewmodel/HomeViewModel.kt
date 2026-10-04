package com.venom.wordvibe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venom.data.repo.UserActivityRepository
import com.venom.data.repo.UserWordProgressRepository
import com.venom.domain.model.DashboardData
import com.venom.domain.model.WordMaster
import com.venom.domain.repo.IUserIdentityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Phase 1 home: greeting, streak, today's session card (due + new + rough minutes),
 * and the needs-attention rail — all deterministic reads over existing projections.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val identityRepository: IUserIdentityRepository,
    private val activityRepository: UserActivityRepository,
    private val progressRepository: UserWordProgressRepository,
) : ViewModel() {

    data class HomeUiState(
        val isLoading: Boolean = true,
        val dashboard: DashboardData? = null,
        val newWordsCount: Int = 0,
        val attentionWords: List<WordMaster> = emptyList(),
    )

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                val userId = identityRepository.getCurrentUserId()
                val dashboard = activityRepository.getDashboardData(userId)
                val newCount = progressRepository.countNewWordsForUser(userId)
                val attention = progressRepository.getWeakProductionWords(userId, 3)
                _state.value = HomeUiState(
                    isLoading = false,
                    dashboard = dashboard,
                    newWordsCount = newCount,
                    attentionWords = attention,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value = HomeUiState(isLoading = false)
            }
        }
    }

    /** Rough planning estimate; the session itself is bounded by the deck, not this. */
    fun estimatedMinutes(due: Int, new: Int): Int = ((due * 0.5) + (new * 0.75)).toInt().coerceAtLeast(1)
}
