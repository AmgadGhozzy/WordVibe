package com.venom.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Rate-limiting / quota tracker per user per day.
 * Used for AI call quotas, translation limits, etc.
 * Upsert pattern: INSERT OR REPLACE with atomic counter increments.
 */
@Entity(
    tableName = "usage_daily",
    primaryKeys = ["userId", "dateKey"],
    indices = [
        Index(value = ["userId", "dateKey"])
    ]
)
data class UsageDailyEntity(
    val userId: String,
    val dateKey: String,         // "yyyy-MM-dd"

    val translations: Int = 0,
    val aiCalls: Int = 0,
    val ocrScans: Int = 0,

    val updatedAt: Long = System.currentTimeMillis()
)
