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
        Index(value = ["synced"]),
        Index(value = ["idempotencyKey"], unique = true)
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
     * Null for rows written before keys existed; nulls never match a real key and are
     * skipped by replay. DB-enforced UNIQUE (multiple nulls allowed by SQLite).
     */
    val idempotencyKey: String? = null,
    /** Owning session id (tapKey session part), for session-scoped queries and debugging. */
    val sessionId: String? = null,
    /** True once the projection (progress/SRS/XP) has been applied for this event. */
    val projectionApplied: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
