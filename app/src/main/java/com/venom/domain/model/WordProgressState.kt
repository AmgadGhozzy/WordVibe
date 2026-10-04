package com.venom.domain.model

data class WordProgressState(
    val completed: Int = 0,
    val total: Int = 0,
    val mastered: Int = 0,
    val learning: Int = 0,
    val needsReview: Int = 0,
    val currentStreak: Int = 0,
    val xpEarned: Int = 0,
    val xpToNextLevel: Int = 0,
    val sessionDurationMs: Long = 0L,
    val totalSessionCount: Int = 0
)
