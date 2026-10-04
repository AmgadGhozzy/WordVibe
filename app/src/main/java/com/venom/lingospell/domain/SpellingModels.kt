package com.venom.lingospell.domain

import java.util.UUID

/**
 * Status of a letter in the letter bank.
 */
enum class LetterStatus {
    AVAILABLE,  // Can be selected
    USED,       // Already placed in a slot
    HIDDEN      // Hidden from view (hint level 3)
}

/**
 * A letter tile in the letter bank.
 */
data class Letter(
    val id: String = UUID.randomUUID().toString(),
    val char: Char,
    val status: LetterStatus = LetterStatus.AVAILABLE
)

/**
 * Status of a word slot.
 */
enum class SlotStatus {
    EMPTY,      // No letter placed
    FILLED,     // Letter placed, not yet validated
    CORRECT,    // Letter is correct
    ERROR       // Letter is incorrect
}

/**
 * A slot where a letter can be placed.
 */
data class Slot(
    val id: String,
    val letter: Letter? = null,
    val isLocked: Boolean = false,
    val status: SlotStatus = SlotStatus.EMPTY
)

/**
 * Available hint levels.
 */
enum class HintLevel(val value: Int) {
    NONE(0),
    LEVEL_1(1),  // Reveal first incorrect position
    LEVEL_2(2),  // Fill all but one position
    LEVEL_3(3);  // Hide distractors

    val remaining: Int get() = 3 - value
    val canUseHint: Boolean get() = value < 3

    fun next(): HintLevel = when (this) {
        NONE -> LEVEL_1
        LEVEL_1 -> LEVEL_2
        LEVEL_2 -> LEVEL_3
        LEVEL_3 -> LEVEL_3
    }
}

/**
 * Feedback state after word validation.
 */
enum class FeedbackState {
    IDLE,       // Waiting for input
    SUCCESS,    // Word spelled correctly
    ERROR,      // Word spelled incorrectly
    MASTERED    // Word mastered (streak complete)
}
