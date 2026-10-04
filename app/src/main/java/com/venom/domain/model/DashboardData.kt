package com.venom.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class DashboardData(
    val totalXp: Int = 0,
    val todayXp: Int = 0,
    val level: UserLevel = UserLevel.NEWCOMER,
    val levelProgress: Float = 0f,
    val xpToNextLevel: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalWordsViewed: Int = 0,
    val totalWordsLearned: Int = 0,
    val totalWordsMastered: Int = 0,
    val totalWordsLearning: Int = 0,
    val totalWordsNeedsReview: Int = 0,
    val totalSessionCount: Int = 0,
    val totalTimeMs: Long = 0,
    val totalDaysActive: Int = 0,
    val dailyGoalTarget: Int = 50,
    val dailyGoalProgress: Int = 0,
    val dailyGoalMet: Boolean = false,
    val streakFreezes: Int = 0
)