package com.venom.domain.model

object StreakEngine {

    /**
     * Consecutive active days ending today (or yesterday when today is quiet).
     * A lone day after a proven break starts nothing: continuity must re-establish.
     */
    fun calculateStreak(
        activeDays: List<String>,
        today: String,
        yesterday: String,
        getPrevious: (String) -> String
    ): Int {
        if (activeDays.isEmpty()) return 0

        val first = activeDays.first()
        if (first != today && first != yesterday) return 0

        var streak = 0
        var expected = if (first == today) today else yesterday
        var consumed = 0

        for (day in activeDays) {
            if (day != expected) break
            streak++
            expected = getPrevious(expected)
            consumed++
        }
        if (consumed < activeDays.size && streak <= 1) return 0
        return streak
    }

    /**
     * A freeze bridges exactly one missed day: the most recent active day is precisely
     * the day before yesterday. Anything older is unbridgeable — no freeze is consumed.
     */
    fun shouldConsumeFreeze(
        activeDays: List<String>,
        today: String,
        yesterday: String,
        getPrevious: (String) -> String
    ): Boolean {
        if (activeDays.isEmpty()) return false
        val first = activeDays.first()
        if (first == today || first == yesterday) return false
        return first == getPrevious(yesterday)
    }

    fun checkMilestone(streak: Int): StreakMilestone? =
        StreakMilestone.entries.firstOrNull { it.days == streak }
}

enum class StreakMilestone(
    val days: Int,
    val title: String,
    val bonusXp: Int
) {
    WEEK        (7,   "1 Week Streak!",    50),
    TWO_WEEKS   (14,  "2 Week Streak!",    100),
    MONTH       (30,  "Monthly Master!",   200),
    TWO_MONTHS  (60,  "Unstoppable!",      350),
    HUNDRED     (100, "Century Club!",     500),
    YEAR        (365, "Legendary!",        1000)
}