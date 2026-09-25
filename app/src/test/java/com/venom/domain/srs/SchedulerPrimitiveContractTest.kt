package com.venom.domain.srs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Ticket 04d golden contracts: the scheduling primitive consumes frozen quality and maps
 * it deterministically to review dates. No policy lives here — tiers and factors below
 * are the frozen primitive, changeable only by reopening the freeze.
 */
class SchedulerPrimitiveContractTest {

    private val fixedClockEngine = SrsEngine(clock = { 1_000_000L })
    private val hourMs = 3_600_000L

    /** Tier expectation at difficulty 5 (factor 0.75), in exact milliseconds. */
    private fun expectReviewAt(stability: Float, baseHours: Long) {
        assertEquals(
            1_000_000L + baseHours * hourMs * 3 / 4,
            fixedClockEngine.calculateNextReview(stability, 5)
        )
    }

    @Test
    fun `tiers map stability bands to base intervals`() {
        // difficulty 5 -> factor 0.75 throughout; exact millisecond arithmetic.
        expectReviewAt(0.5f, 4)
        expectReviewAt(1.5f, 24)
        expectReviewAt(3.0f, 72)
        expectReviewAt(5.0f, 168)
        expectReviewAt(8.0f, 336)
    }

    @Test
    fun `higher stability schedules later at fixed difficulty`() {
        val low = fixedClockEngine.calculateNextReview(0.5f, 5)
        val mid = fixedClockEngine.calculateNextReview(3.0f, 5)
        val high = fixedClockEngine.calculateNextReview(8.0f, 5)

        assertTrue(low < mid && mid < high, "stronger signals must review later")
    }

    @Test
    fun `higher difficulty schedules sooner at fixed stability`() {
        val easy = fixedClockEngine.calculateNextReview(3.0f, 1)
        val mid = fixedClockEngine.calculateNextReview(3.0f, 5)
        val hard = fixedClockEngine.calculateNextReview(3.0f, 10)

        assertTrue(easy > mid && mid > hard, "harder words must review sooner")
    }

    @Test
    fun `identical inputs schedule identically`() {
        assertEquals(
            fixedClockEngine.calculateNextReview(2.5f, 4),
            fixedClockEngine.calculateNextReview(2.5f, 4),
            "scheduler output is fully deterministic under a fixed clock"
        )
    }
}
