package com.venom.wordvibe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venom.data.repo.UserWordProgressRepository
import com.venom.domain.model.KnownState
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
 * Phase 2 writing tab: one spelling queue that PREFERS production-weak words —
 * where the recorded production attempts exceed the production successes
 * (ADR-0008). Ordering is derived from read-only event aggregates; no schema
 * change, no contract surface. Picking a word opens the typing editor.
 */
import com.venom.analytics.CrashlyticsManager
import com.venom.analytics.ext.logApiError

@HiltViewModel
class SpellingQueueViewModel @Inject constructor(
    private val identityRepository: IUserIdentityRepository,
    private val progressRepository: UserWordProgressRepository,
    private val crashlyticsManager: CrashlyticsManager,
) : ViewModel() {

    data class QueueItem(
        val word: WordMaster,
        val isDue: Boolean,
        val productionWeak: Boolean,
    )

    data class QueueUiState(
        val isLoading: Boolean = true,
        val items: List<QueueItem> = emptyList(),
        val selectedWord: WordMaster? = null,
    )

    private val _state = MutableStateFlow(QueueUiState())
    val state: StateFlow<QueueUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                val userId = identityRepository.getCurrentUserId()
                val due = progressRepository.getWordsDueForReview(userId, 10)
                val weak = progressRepository.getWeakProductionWords(userId, 10)
                    .filter { w -> due.none { it.id == w.id } }
                val candidates = (due.map { it to true } + weak.map { it to false })

                val attemptCounts = progressRepository.getProductionAttemptCounts(userId)
                val items = candidates.map { (word, isDue) ->
                    val progress = progressRepository.getDisplayProgress(userId, word.id)
                    val attempts = attemptCounts[word.id] ?: 0
                    QueueItem(
                        word = word,
                        isDue = isDue,
                        productionWeak = progress.productionSuccess < attempts,
                    )
                }.sortedWith(
                    compareByDescending<QueueItem> { it.productionWeak }
                        .thenByDescending { it.isDue }
                )

                _state.value = QueueUiState(isLoading = false, items = items)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                crashlyticsManager.logNonFatalException(e, "queue load failed")
                _state.value = QueueUiState(isLoading = false)
            }
        }
    }

    fun pick(word: WordMaster) {
        _state.value = _state.value.copy(selectedWord = word)
    }

    fun dismissEditor() {
        _state.value = _state.value.copy(selectedWord = null)
        load()
    }
}
