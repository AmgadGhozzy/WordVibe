package com.venom.domain.srs

import com.venom.domain.model.UserWordProgress
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Ticket 04c golden contracts (RED then green): frozen quality mapping.
 * Recall failure is a strong negative (LOWER quality) vs production failure which is
 * negative (LOW quality). Scheduler stays a dumb deterministic primitive — these tests
 * pin the mapping only, never scheduling tiers.
 */
class QualityMappingContractTest {

    private val engine = SrsEngine()

    private fun stableWord(stability: Float = 5.0f) = UserWordProgress(
        userId = "u",
        wordId = 1,
        recallSuccess = 2,
        productionSuccess = 2,
        totalAttempts = 4,
        difficulty = 5,
        stability = stability,
        lapsesCount = 0,
        // Ticket 08: firstSeen's default is System.currentTimeMillis() at construction,
        // so two freshly built words differed by a millisecond and made the
        // determinism assertion a wall-clock race. Pin it like the engine's clock.
        firstSeen = 1_000_000L
    )

    @Test
    fun `recall failure hurts stability more than production failure`() {
        val afterRecall = engine.onRecallFail(stableWord())
        val afterProduction = engine.onProductionFail(stableWord())

        assertEquals(3.0f, afterRecall.stability, "recall failure applies exactly -2.0")
        assertEquals(4.0f, afterProduction.stability, "production failure applies exactly -1.0")
        assertTrue(
            afterRecall.stability < afterProduction.stability,
            "recall failure is the stronger negative"
        )
    }

    @Test
    fun `recall failure schedules sooner than production failure`() {
        val afterRecall = engine.onRecallFail(stableWord())
        val afterProduction = engine.onProductionFail(stableWord())

        assertTrue(
            (afterRecall.nextReview ?: Long.MAX_VALUE) <
                (afterProduction.nextReview ?: Long.MAX_VALUE),
            "stronger negative must review sooner"
        )
    }

    @Test
    fun `both failures raise difficulty and count the lapse`() {
        val start = stableWord()

        listOf(engine.onRecallFail(start), engine.onProductionFail(start)).forEach { out ->
            assertEquals(6, out.difficulty, "failure hardens difficulty by exactly one")
            assertEquals(1, out.lapsesCount, "failure counts exactly one lapse")
            assertEquals(5, out.totalAttempts, "failure counts exactly one attempt")
        }
    }

    @Test
    fun `success weights stay frozen`() {
        val base = UserWordProgress(userId = "u", wordId = 1)
        // Base stability for rank 1000 / frequency 1: (0)*0.3 + (1/6)*0.2.
        val expectedBase = 1f / 30f

        val recallOnly = engine.onRecallSuccess(base, rank = 1000, frequency = 1)
        val productionOnly = engine.onProductionSuccess(base, rank = 1000, frequency = 1)

        assertEquals(1.2f, recallOnly.stability - expectedBase, 1e-4f)
        assertEquals(2.0f, productionOnly.stability - expectedBase, 1e-4f)
        assertTrue(
            productionOnly.stability > recallOnly.stability,
            "production success outweighs one recall success by frozen weights"
        )
    }

    @Test
    fun `lapse penalty isolates to exactly 1_5 per lapse`() {
        // High base keeps both outcomes clear of the zero floor.
        val clean = stableWord().copy(lapsesCount = 0)
        val lapsed = stableWord().copy(lapsesCount = 2)

        val cleanOut = engine.onRecallSuccess(clean, rank = 1000, frequency = 1)
        val lapsedOut = engine.onRecallSuccess(lapsed, rank = 1000, frequency = 1)

        assertEquals(
            3.0f,
            cleanOut.stability - lapsedOut.stability,
            1e-4f,
            "two lapses must cost exactly 2 x 1.5 stability"
        )
    }

    @Test
    fun `mapping is deterministic`() {
        val fixed = SrsEngine(clock = { 1_000_000L })
        assertEquals(
            fixed.onRecallFail(stableWord()),
            fixed.onRecallFail(stableWord()),
            "identical signals must map identically, timestamps included"
        )
        assertEquals(
            fixed.onProductionSuccess(stableWord(), rank = 500, frequency = 3),
            fixed.onProductionSuccess(stableWord(), rank = 500, frequency = 3),
            "identical successes must map identically, timestamps included"
        )
    }
}
