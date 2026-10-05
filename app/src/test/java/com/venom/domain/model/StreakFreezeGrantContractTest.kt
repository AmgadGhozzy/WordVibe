package com.venom.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * ADR-0010 freeze-grant contract: one freeze per multiple of 7 of the honest
 * streak, capped at 2, never twice for the same multiple. The caller passes the
 * streak observed at the previous evaluation as lastGrantedAtStreak; because a
 * real study day advances the streak by exactly one, a multiple is reached (and
 * therefore granted) at most once per climb.
 */
class StreakFreezeGrantContractTest {

    @Test
    fun `grants one freeze at the first seven-day multiple`() {
        assertEquals(1, StreakEngine.freezeGrant(7, freezes = 0, lastGrantedAtStreak = 6))
    }

    @Test
    fun `grants a second freeze at the fourteenth day`() {
        assertEquals(2, StreakEngine.freezeGrant(14, freezes = 1, lastGrantedAtStreak = 7))
    }

    @Test
    fun `cap of two freezes is never exceeded`() {
        assertEquals(2, StreakEngine.freezeGrant(14, freezes = 2, lastGrantedAtStreak = 13))
        assertEquals(2, StreakEngine.freezeGrant(21, freezes = 2, lastGrantedAtStreak = 20))
    }

    @Test
    fun `the same multiple is never granted twice`() {
        assertEquals(1, StreakEngine.freezeGrant(7, freezes = 1, lastGrantedAtStreak = 7))
        assertEquals(2, StreakEngine.freezeGrant(14, freezes = 2, lastGrantedAtStreak = 14))
    }

    @Test
    fun `non-multiples and zero streaks grant nothing`() {
        assertEquals(0, StreakEngine.freezeGrant(6, freezes = 0, lastGrantedAtStreak = 5))
        assertEquals(0, StreakEngine.freezeGrant(8, freezes = 0, lastGrantedAtStreak = 7))
        assertEquals(
            1, StreakEngine.freezeGrant(20, freezes = 1, lastGrantedAtStreak = 19),
            "a non-multiple returns the balance unchanged"
        )
        assertEquals(0, StreakEngine.freezeGrant(0, freezes = 0, lastGrantedAtStreak = 0))
    }

    @Test
    fun `a stale higher marker blocks the grant conservatively`() {
        // Marker above the current streak can only mean the profile write lagged;
        // granting anyway could double-credit the same climb.
        assertEquals(0, StreakEngine.freezeGrant(7, freezes = 0, lastGrantedAtStreak = 20))
    }
}
