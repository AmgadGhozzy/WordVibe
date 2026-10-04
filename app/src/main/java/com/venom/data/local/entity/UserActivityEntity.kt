package com.venom.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "userActivity",
    primaryKeys = ["userId", "dateKey"],
    indices = [
        Index(value = ["userId", "dateKey"])
    ]
)
data class UserActivityEntity(
    val userId: String,
    val dateKey: String,

    // Daily counters
    val wordsViewed: Int = 0,
    val wordsPracticed: Int = 0,
    val sessionCount: Int = 0,
    val totalTimeMs: Long = 0,

    // Quiz / SRS
    val recallSuccess: Int = 0,
    val recallFail: Int = 0,
    val productionSuccess: Int = 0,

    val masteredCount: Int = 0,
    val totalXpEarned: Int = 0,
    val dailyGoalTarget: Int = 50,
    val dailyGoalMet: Boolean = false,

    // Meta
    val updatedAt: Long = System.currentTimeMillis()
)