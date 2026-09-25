package com.venom.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Tickets 01/05 streak contracts: continuity breaks to zero on unprotected gaps, and a
 * freeze bridges exactly one missed day. Unbroken chains count fully.
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

        val consume = StreakEngine.shouldConsumeFreeze(activeDays, today, yesterday, ::previousDay)

        assertEquals(false, consume, "a freeze must only bridge exactly one missed day")
    }

    @Test
    fun `freeze is consumed for exactly one missed day`() {
        val today = day(0)
        val yesterday = day(-1)
        val activeDays = listOf(day(-2))

        val consume = StreakEngine.shouldConsumeFreeze(activeDays, today, yesterday, ::previousDay)

        assertEquals(true, consume, "a single missed day is bridgeable")
    }

    @Test
    fun `unbroken chains count fully`() {
        val today = day(0)
        val yesterday = day(-1)

        assertEquals(
            3,
            StreakEngine.calculateStreak(
                listOf(today, yesterday, day(-2)), today, yesterday, ::previousDay
            )
        )
        assertEquals(
            1,
            StreakEngine.calculateStreak(listOf(today), today, yesterday, ::previousDay),
            "first day ever starts at one"
        )
        assertEquals(
            0,
            StreakEngine.calculateStreak(emptyList(), today, yesterday, ::previousDay)
        )
    }
}
