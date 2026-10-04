package com.venom.domain.model

data class XpResult(
    val baseXp: Int,
    val streakMultiplier: Float,
    val totalXp: Int,
    val newTotalXp: Int,
    val newLevel: UserLevel,
    val leveledUp: Boolean,
    val dailyGoalJustMet: Boolean,
    val streakMilestone: StreakMilestone? = null
)