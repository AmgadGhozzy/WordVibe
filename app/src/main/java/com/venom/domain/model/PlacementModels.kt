package com.venom.domain.model

import androidx.compose.runtime.Immutable

object PlacementLevels {

    private val ordered = WordLevels.values()

    fun levels(): List<WordLevels> = ordered

    fun cefrFor(level: WordLevels): String = when (level) {
        is WordLevels.Beginner -> "A1"
        is WordLevels.Elementary -> "A2"
        is WordLevels.Intermediate -> "B1"
        is WordLevels.UpperIntermediate -> "B2"
        is WordLevels.Advanced -> "C1"
        is WordLevels.Mastery -> "C2"
    }

    fun indexOf(level: WordLevels): Int = ordered.indexOf(level)

    fun levelAt(index: Int): WordLevels = ordered[index.coerceIn(ordered.indices)]

    /**
     * Maximum level assignable through placement test.
     * C1/C2 must be unlocked only through progression.
     */
    val MAX_PLACEMENT_INDEX = ordered.indexOf(WordLevels.UpperIntermediate) // B2 = index 3

    /**
     * Calculates the assigned level from placement results.
     *
     * Strategy: The highest level where the user answered correctly,
     * capped at B2 (index 3). If the user fails all questions, they get A1.
     * Levels never attempted (below the corpus-floor ladder start, ADR-0010) are
     * skipped: only an attempted failure cuts the adaptive scan.
     *
     * Returns a pair of (WordLevels, CEFR string).
     */
    fun calculateAssignedLevel(levelResults: Map<String, Boolean>): Pair<WordLevels, String> {
        // Find the highest level passed (last correct answer in ordered list)
        var highestPassedIndex = -1
        for ((index, level) in ordered.withIndex()) {
            val result = levelResults[level.id]
            when {
                // Unattempted level (below the ladder start): not evidence either way.
                result == null -> continue
                result -> highestPassedIndex = index
                // Stop at first failure — adaptive cutoff
                else -> break
            }
        }

        // If no level passed, assign Beginner
        if (highestPassedIndex < 0) {
            return WordLevels.Beginner to cefrFor(WordLevels.Beginner)
        }

        // Cap at B2
        val cappedIndex = highestPassedIndex.coerceAtMost(MAX_PLACEMENT_INDEX)
        val assigned = levelAt(cappedIndex)
        return assigned to cefrFor(assigned)
    }
}

@Immutable
data class LevelScore(
    val levelId: String,
    val correct: Int,
    val total: Int
) {
    val accuracy: Float get() = if (total == 0) 0f else correct.toFloat() / total
}

@Immutable
data class PlacementResult(
    val assignedLevel: WordLevels,
    val estimatedCefr: String,
    val totalCorrect: Int,
    val totalQuestions: Int,
    val levelScores: List<LevelScore>,
    val durationMs: Long
)

/**
 * Builds the placement result from answered questions only. Unattempted levels never
 * appear: numerator and denominator both count questions, never levels.
 * Pure function, unit-tested.
 */
fun buildPlacementResult(
    allLevels: List<WordLevels>,
    levelResults: Map<String, Boolean>,
    answeredQuestions: Int,
    totalCorrect: Int,
    durationMs: Long
): PlacementResult {
    val scores = allLevels
        .filter { it.id in levelResults }
        .map { level ->
            val correct = levelResults[level.id] == true
            LevelScore(level.id, if (correct) 1 else 0, 1)
        }
    val (assignedLevel, cefr) = PlacementLevels.calculateAssignedLevel(levelResults)
    return PlacementResult(
        assignedLevel = assignedLevel,
        estimatedCefr = cefr,
        totalCorrect = totalCorrect,
        totalQuestions = answeredQuestions,
        levelScores = scores,
        durationMs = durationMs
    )
}
