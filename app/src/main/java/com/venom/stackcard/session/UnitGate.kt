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

    /**
     * Fixed sequential chain over the ten canonical IELTS topics in the
     * editorial order pinned by ADR-0011 Decision 4.
     */
    val EDITORIAL_CHAIN: List<String> = listOf(
        "Education",
        "Work",
        "Health",
        "Travel",
        "Technology",
        "Environment",
        "Culture",
        "Society",
        "Economy",
        "Science"
    )

    /**
     * Resolves the currently-open unit along [EDITORIAL_CHAIN], starting from
        * [persistedUnit] (or the first unit when null/unrecognized) and advancing
        * monotonically while the current unit meets the 60% known gate.
        *
        * [unitCounts] returns `(knownCount, usableCount)` for a given topic.
        */
    fun resolveOpenUnit(
        persistedUnit: String?,
        unitCounts: (String) -> Pair<Int, Int>
    ): String {
        var idx = persistedUnit
            ?.let { unit -> EDITORIAL_CHAIN.indexOfFirst { it.equals(unit, ignoreCase = true) } }
            ?.takeIf { it >= 0 }
            ?: 0

        while (idx < EDITORIAL_CHAIN.lastIndex) {
            val (known, usable) = unitCounts(EDITORIAL_CHAIN[idx])
            if (usable > 0) {
                if (shouldUnlockNext(known, usable)) {
                    idx++
                } else {
                    break
                }
            } else {
                val hasLaterUsable = ((idx + 1)..EDITORIAL_CHAIN.lastIndex).any { laterIdx ->
                    unitCounts(EDITORIAL_CHAIN[laterIdx]).second > 0
                }
                if (hasLaterUsable && shouldUnlockNext(known, usable)) {
                    idx++
                } else {
                    break
                }
            }
        }
        return EDITORIAL_CHAIN[idx]
    }

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
