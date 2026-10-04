package com.venom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "userProfile")
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val totalXp: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastActiveDate: String? = null,
    val dailyGoalXp: Int = 50,
    val streakFreezes: Int = 0,
    val placementUnit: Int = 0,
    val placementCefr: String? = null,
    val placementTakenAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
