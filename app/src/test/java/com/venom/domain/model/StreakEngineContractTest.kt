package com.venom.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Ticket 01 golden contracts (RED): streak continuity.
 *
 * Frozen contract: an unprotected gap resets the streak to zero, and a freeze is
 * consumed only for a bridgeable (exactly one missed day) gap. These tests assert
 * externally observable streak behavior, never implementation internals.
 */
class StreakEngineContractTest {

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private fun day(offsetDays: Long): String =
        LocalDate.now().plusDays(offsetDays).format(fmt)

    private fun previousDay(dateKey: String): String =
        LocalDate.parse(dateKey, fmt).minusDays(1).format(fmt)

    @Test
    fun `unprotected far gap with today active resets streak to zero`() {
        val today = day(0)
        val yesterday = day(-1)
        // Active today, but the previous active day is 3 days ago: continuity is broken.
        val activeDays = listOf(today, day(-3))

        val streak = StreakEngine.calculateStreak(activeDays, today, yesterday, ::previousDay)

        assertEquals(0, streak, "a broken chain must read 0, today starts nothing by itself")
    }

    @Test
    fun `single missed day without freeze resets streak to zero`() {
        val today = day(0)
        val yesterday = day(-1)
        // Active today and the two days before yesterday: yesterday itself is missing.
        val activeDays = listOf(today, day(-2), day(-3))

        val streak = StreakEngine.calculateStreak(activeDays, today, yesterday, ::previousDay)

        assertEquals(0, streak, "one missed day without a freeze breaks continuity")
    }

    @Test
    fun `freeze is not consumed for an unbridgeable gap`() {
        val today = day(0)
        val yesterday = day(-1)
        // Most recent activity is 3 days ago: no single freeze can bridge this.
        val activeDays = listOf(day(-3))

        val consume = StreakEngine.shouldConsumeFreeze(activeDays, today, yesterday)

        assertEquals(false, consume, "a freeze must only bridge exactly one missed day")
    }
}
