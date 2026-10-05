package com.venom.stackcard.session

/**
 * Pure unit-unlock decisions (ADR-0011, accepted 2026-10-06). Units are the
 * ten canonical IELTS topics in the editorial order pinned in the ADR; the
 * next unit opens when ceil(60%) of the current unit's usable words are KNOWN
 * or MASTERED. This gate rules on new-word entry only — due review serving is
 * never locked (the frozen due definition is untouched). The first unit is
 * always open, and placement completion never gates anything: an unplaced
 * user simply starts at unit 1. Chain composition (which unit is "current")
 * happens in the wiring ticket; this object holds only the atomic decision.
 */
object UnitGate {

    const val OPEN_RATIO_PERCENT = 60

    /** Words that must be known before the next unit opens (rounded up). */
    fun requiredKnownCount(unitSize: Int): Int =
        ((unitSize.toLong() * OPEN_RATIO_PERCENT + 99) / 100).toInt()

    /**
     * The atomic unlock decision for the unit after a completed one: open
     * exactly when the known count reaches the rounded-up 60% requirement.
     */
    fun shouldUnlockNext(knownCount: Int, unitSize: Int): Boolean =
        knownCount >= requiredKnownCount(unitSize)

    /** UI-facing lock reason: "اعرف N كلمات أخرى لتفتح وحدة X". */
    data class LockReason(
        val requiredKnownCount: Int,
        val knownCount: Int,
    ) {
        val remaining: Int get() = (requiredKnownCount - knownCount).coerceAtLeast(0)
    }

    /** Null when the next unit is open; otherwise the reason data. */
    fun lockReasonFor(knownCount: Int, unitSize: Int): LockReason? =
        if (shouldUnlockNext(knownCount, unitSize)) null
        else LockReason(requiredKnownCount(unitSize), knownCount)
}
