package com.venom.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Immutable record of every quiz / flashcard attempt.
 * Used for analytics, detailed progress tracking, and syncing to Supabase.
 */
@Entity(
    tableName = "quiz_attempts",
    indices = [
        Index(value = ["userId", "wordId"]),
        Index(value = ["userId", "dateKey"]),
        Index(value = ["synced"])
    ]
)
data class QuizAttemptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: String,
    val wordId: Int,
    val dateKey: String,         // "yyyy-MM-dd"
    val questionType: String,    // "MEANING", "DEFINITION", "REVERSE", "RECALL", "PRODUCTION"
    val source: String,          // "FLASHCARD", "UNIT", "PLACEMENT"
    val isCorrect: Boolean,
    val selectedAnswer: String,
    val correctAnswer: String,
    val timeSpentMs: Long = 0,

    val synced: Boolean = false,
    /**
     * Dedupe key for exactly-once session commits: session + word + outcome + tap ordinal.
     * Null for rows written before keys existed; nulls never match a real key.
     */
    val idempotencyKey: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
