package com.venom.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Immutable event log for every XP award.
 * The authoritative total XP = SUM(amount) for a user.
 * The cached value in [UserProfileEntity.totalXp] is periodically reconciled.
 */
@Entity(
    tableName = "xp_events",
    indices = [
        Index(value = ["userId", "dateKey"]),
        Index(value = ["userId", "createdAt"])
    ]
)
data class XpEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: String,
    val dateKey: String,        // "yyyy-MM-dd"
    val source: String,          // e.g. "word_view", "recall_success", "daily_goal_bonus", "streak_milestone"
    val baseXp: Int,
    val streakMultiplier: Float = 1f,
    val amount: Int,             // = (baseXp * streakMultiplier).toInt()
    val wordId: Int? = null,     // nullable — not every XP event is word-specific
    val synced: Boolean = false, // false until pushed to remote
    val createdAt: Long = System.currentTimeMillis()
)
