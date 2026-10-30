package com.venom.wordvibe.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venom.data.repo.UserWordProgressRepository
import com.venom.domain.model.WordMaster
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * app03 §02: read-only unit preview lesson. Loads the usable words of the
 * routed topic (rank-ascending) plus the two untracked challenges (synonym
 * pick, recall reveal) built deterministically in UnitsLessonLogic. Writes
 * nothing by design — the PM decision "درس المعاينة لا يكتب في أي جدول".
 */
@HiltViewModel
class UnitsLessonViewModel @Inject constructor(
    private val repository: UserWordProgressRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** Nav route arg: the canonical EDITORIAL_CHAIN topic this lesson serves. */
    val topic: String = savedStateHandle.get<String>("topic").orEmpty()

    data class LessonUiState(
        val isLoading: Boolean = true,
        val words: List<WordMaster> = emptyList(),
        val synonymChallenges: Map<Int, SynonymChallenge> = emptyMap(),
    )

    private val _state = MutableStateFlow(LessonUiState())
    val state: StateFlow<LessonUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                val byTopic = usableWordsForTopic(repository.getAllWordsForLesson(), topic)
                val challenges = byTopic.indexedChallenges()
                _state.value = LessonUiState(isLoading = false, words = byTopic, synonymChallenges = challenges)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value = LessonUiState(isLoading = false)
            }
        }
    }
}

/** Deterministic per-index challenge pass; unanswerable entries are simply absent. */
private fun List<WordMaster>.indexedChallenges(): Map<Int, SynonymChallenge> =
    mapIndexed { index, word -> synonymChallengeFor(this, index)?.let { word.id to it } }
        .filterNotNull()
        .toMap()
