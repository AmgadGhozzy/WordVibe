package com.venom.lingospell.domain

import java.util.UUID

object SpellingUtils {

    private val ALPHABET = ('A'..'Z').toList()

    /**
     * Generates a unique identifier.
     */
    fun generateId(): String = UUID.randomUUID().toString().take(9)

    /**
     * Generates the letter bank with target letters plus distractors.
     *
     * @param targetWord The word to spell (will be converted to uppercase)
     * @return Shuffled list of letters including target chars and random distractors
     */
    fun generateBank(targetWord: String, distractorCount: Int): List<Letter> {
        val targetChars = targetWord.uppercase().toList()

        // Generate random distractor letters
        val distractors = (1..distractorCount).map {
            ALPHABET.random()
        }

        val allChars = targetChars + distractors

        return allChars.shuffled().map { char ->
            Letter(
                id = generateId(),
                char = char,
                status = LetterStatus.AVAILABLE
            )
        }
    }

    /**
     * Creates initial empty slots for the target word.
     *
     * @param targetWord The word to create slots for
     * @return List of empty slots, one per character
     */
    fun createSlots(targetWord: String): List<Slot> {
        return targetWord.uppercase().mapIndexed { index, char ->
            Slot(
                id = "$index-$char",
                letter = null,
                isLocked = false,
                status = SlotStatus.EMPTY
            )
        }
    }
}
