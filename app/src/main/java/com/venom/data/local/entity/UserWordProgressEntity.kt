package com.venom.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "userWordProgress",
    primaryKeys = ["userId", "wordId"],
    indices = [
        Index(value = ["userId", "nextReview"]),
        Index(value = ["userId", "knownState"])
    ]
)
data class UserWordProgressEntity(
    val userId: String,
    val wordId: Int,

    // Exposure
    val firstSeen: Long = System.currentTimeMillis(),

    // Interaction
    val bookmarked: Boolean = false,

    // Practice totals
    val productionSuccess: Int = 0,
    val recallSuccess: Int = 0,
    val totalAttempts: Int = 0,

    // Learning state
    val knownState: String = "NEW",

    // SRS
    val difficulty: Int = 5,
    val stability: Float = 0f,
    val lapsesCount: Int = 0,
    val lastReview: Long? = null,
    val nextReview: Long? = null
)
