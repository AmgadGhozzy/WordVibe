package com.venom.lingospell.presentation

import com.venom.domain.model.WordMaster
import com.venom.lingospell.domain.FeedbackState
import com.venom.lingospell.domain.HintLevel
import com.venom.lingospell.domain.Letter
import com.venom.lingospell.domain.Slot

enum class SpellingGameMode {
    TRAINING,
    TEST
}

/**
 * Immutable UI state for the Spelling Game.
 */
data class SpellingGameState(
    val mode: SpellingGameMode = SpellingGameMode.TRAINING,
    val wordIndex: Int = 0,
    val streak: Int = 0,
    val mistakes: Int = 0,
    val hintLevel: HintLevel = HintLevel.NONE,
    val slots: List<Slot> = emptyList(),
    val bank: List<Letter> = emptyList(),
    val feedback: FeedbackState = FeedbackState.IDLE,
    val shakeTrigger: Int = 0,
    val isLoading: Boolean = true,
    val overrideWord: WordMaster? = null,
    val showMasteryDialog: Boolean = false
) {
    /**
     * Current word being spelled.
     */
    val currentWord: WordMaster?
        get() = overrideWord

    /**
     * Whether hint button should be visible.
     * Shows after first mistake, when idle, and hints remaining.
     */
    val showHintButton: Boolean
        get() = mode == SpellingGameMode.TRAINING && mistakes > 0 && feedback == FeedbackState.IDLE && hintLevel.canUseHint

    /**
     * Whether game input is blocked (during success/mastered states).
     */
    val isInputBlocked: Boolean
        get() = feedback == FeedbackState.SUCCESS || feedback == FeedbackState.MASTERED

    /**
     * First empty slot index, or -1 if all filled.
     */
    val firstEmptySlotIndex: Int
        get() = slots.indexOfFirst { it.letter == null }

    /**
     * Whether all slots are filled.
     */
    val allSlotsFilled: Boolean
        get() = slots.all { it.letter != null }
}

/**
 * Sealed interface for one-time UI events.
 */
sealed interface SpellingGameEvent {
    data class PlayTts(val text: String, val languageTag: String) : SpellingGameEvent
    data object NavigateBack : SpellingGameEvent
}