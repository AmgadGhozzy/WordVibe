package com.venom.quiz.placement

import com.venom.domain.model.PlacementLevels
import com.venom.domain.model.buildPlacementResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Ticket 05: placement scores count answered questions, never the full pool.
 * Unattempted levels never appear in the result.
 */
class PlacementScoringContractTest {

    @Test
    fun `score counts answered questions only`() {
        val levels = PlacementLevels.levels()
        val answered = mapOf(
            levels[0].id to true,
            levels[1].id to false
        )

        val result = buildPlacementResult(
            allLevels = levels,
            levelResults = answered,
            answeredQuestions = 2,
            totalCorrect = 1,
            durationMs = 5_000L
        )

        assertEquals(2, result.totalQuestions, "denominator is answered count, not pool size")
        assertEquals(1, result.totalCorrect, "numerator counts correct questions, not levels")
        assertEquals(
            2, result.levelScores.size,
            "unattempted levels never appear in scores"
        )
        assertTrue(
            result.levelScores.all { it.total == 1 },
            "each attempted level scores exactly one question"
        )
    }

    @Test
    fun `empty attempt scores zero without crashing`() {
        val result = buildPlacementResult(
            allLevels = PlacementLevels.levels(),
            levelResults = emptyMap(),
            answeredQuestions = 0,
            totalCorrect = 0,
            durationMs = 0L
        )

        assertEquals(0, result.totalQuestions)
        assertEquals(0, result.totalCorrect)
        assertTrue(result.levelScores.isEmpty(), "nothing attempted, nothing scored")
    }
}
