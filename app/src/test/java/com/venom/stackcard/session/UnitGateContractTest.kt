package com.venom.stackcard.session

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ADR-0011 (accepted 2026-10-06) contracts for the pure unit gate: the next
 * unit opens at ceil(60%) of the current unit's usable words known, the lock
 * reason exposes a non-negative remainder for the UI, and the decision is
 * atomic — it knows nothing about placement or chain position (those compose
 * in the wiring ticket).
 */
class UnitGateContractTest {

    @Test
    fun `ratio constant is 60`() {
        assertEquals(60, UnitGate.OPEN_RATIO_PERCENT)
    }

    @Test
    fun `required count rounds 60 percent up`() {
        assertEquals(3, UnitGate.requiredKnownCount(5))
        assertEquals(4, UnitGate.requiredKnownCount(6))
        assertEquals(6, UnitGate.requiredKnownCount(10))
        assertEquals(2, UnitGate.requiredKnownCount(3))
        assertEquals(1, UnitGate.requiredKnownCount(1))
        assertEquals(60, UnitGate.requiredKnownCount(100))
        assertEquals(600, UnitGate.requiredKnownCount(999))
    }

    @Test
    fun `zero-size unit never locks - documented edge`() {
        assertEquals(0, UnitGate.requiredKnownCount(0))
        assertTrue(UnitGate.shouldUnlockNext(knownCount = 0, unitSize = 0))
        assertNull(UnitGate.lockReasonFor(knownCount = 0, unitSize = 0))
    }

    @Test
    fun `reaching exactly the required count opens the next unit`() {
        assertTrue(UnitGate.shouldUnlockNext(knownCount = 3, unitSize = 5))
        assertTrue(UnitGate.shouldUnlockNext(knownCount = 6, unitSize = 10))
    }

    @Test
    fun `one below the required count stays locked`() {
        assertFalse(UnitGate.shouldUnlockNext(knownCount = 2, unitSize = 5))
        assertFalse(UnitGate.shouldUnlockNext(knownCount = 5, unitSize = 10))
        assertFalse(UnitGate.shouldUnlockNext(knownCount = 0, unitSize = 7))
    }

    @Test
    fun `open unit produces no lock reason`() {
        assertNull(UnitGate.lockReasonFor(knownCount = 3, unitSize = 5))
        assertNull(UnitGate.lockReasonFor(knownCount = 4, unitSize = 5))
    }

    @Test
    fun `locked unit carries required, known and non-negative remaining`() {
        val reason = UnitGate.lockReasonFor(knownCount = 1, unitSize = 5)!!
        assertEquals(3, reason.requiredKnownCount)
        assertEquals(1, reason.knownCount)
        assertEquals(2, reason.remaining)
    }

    @Test
    fun `remaining never goes negative even with an oversized known count`() {
        val reason = UnitGate.lockReasonFor(knownCount = 2, unitSize = 10)!!
        assertEquals(6, reason.requiredKnownCount)
        assertEquals(2, reason.knownCount)
        assertEquals(4, reason.remaining)
        assertTrue(reason.remaining >= 0)
    }

    @Test
    fun `lock reason is the exact complement of the unlock decision`() {
        for (known in 0..10) {
            val reason = UnitGate.lockReasonFor(knownCount = known, unitSize = 10)
            if (known >= 6) assertNull(reason) else {
                assertEquals(6, reason!!.requiredKnownCount)
                assertEquals(6 - known, reason.remaining)
            }
        }
    }
}
