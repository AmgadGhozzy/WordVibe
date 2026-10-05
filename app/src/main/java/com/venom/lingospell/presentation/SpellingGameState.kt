package com.venom.lingospell.presentation

import com.venom.domain.model.WordMaster
import com.venom.lingospell.domain.FeedbackState
import com.venom.lingospell.domain.HintLevel
import com.venom.lingospell.domain.Letter
import com.venom.lingospell.domain.Slot

/**
 * Immutable UI state for the Spelling Game (ADR-0009: letter bank is the only
 * input surface).
 */
data class SpellingGameState(
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
    /** Celebration at 3 consecutive successes — never a mastery claim (ADR-0009). */
    val showCelebrationDialog: Boolean = false,
    /** Live hint-budget settings snapshot (DataStore-backed, ADR-0009). */
    val hintsEnabled: Boolean = true,
    val maxHintsPerWord: Int = 3,
    val hintCreditPolicy: String = "CREDIT_UP_TO_N",
    val hintCreditLimit: Int = 1
) {
    val currentWord: WordMaster?
        get() = overrideWord

    /** Hint button shows while budget remains, idle, enabled. */
    val showHintButton: Boolean
        get() = hintsEnabled && hintLevel.value < maxHintsPerWord &&
            feedback == FeedbackState.IDLE && hintLevel.canUseHint

    val isInputBlocked: Boolean
        get() = feedback == FeedbackState.SUCCESS || feedback == FeedbackState.MASTERED

    /** Will the current attempt (given hints used so far) earn production credit? */
    val attemptWillCount: Boolean
        get() = when (hintCreditPolicy) {
            "NO_CREDIT_IF_ANY_HINT" -> hintLevel.value == 0
            else -> hintLevel.value <= hintCreditLimit
        }

    val firstEmptySlotIndex: Int
        get() = slots.indexOfFirst { it.letter == null }

    val allSlotsFilled: Boolean
        get() = slots.all { it.letter != null }
}

/**
 * Hint credit policy (ADR-0009).
 */
enum class HintCreditPolicy {
    NO_CREDIT_IF_ANY_HINT,
    CREDIT_UP_TO_N
}

/**
 * Sealed interface for one-time UI events.
 */
sealed interface SpellingGameEvent {
    data class PlayTts(val text: String, val languageTag: String) : SpellingGameEvent
    data object NavigateBack : SpellingGameEvent
}
