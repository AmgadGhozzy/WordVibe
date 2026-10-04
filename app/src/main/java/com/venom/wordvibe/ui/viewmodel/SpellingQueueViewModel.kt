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
 * Phase 1 writing tab: a real spelling queue. Sections read existing projections:
 * due-for-review words and the high-lapse (production-weak) set. Picking a word
 * opens the existing, tested spelling editor for that word.
 */
@HiltViewModel
class SpellingQueueViewModel @Inject constructor(
    private val identityRepository: IUserIdentityRepository,
    private val progressRepository: UserWordProgressRepository,
) : ViewModel() {

    data class QueueUiState(
        val isLoading: Boolean = true,
        val dueWords: List<WordMaster> = emptyList(),
        val weakWords: List<WordMaster> = emptyList(),
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
                _state.value = QueueUiState(isLoading = false, dueWords = due, weakWords = weak)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
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
