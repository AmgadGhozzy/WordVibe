package com.venom.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.venom.data.local.entity.QuizAttemptEntity

@Dao
interface QuizAttemptDao {

    /** Duplicate idempotency keys are a database-level no-op, never an error. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(attempt: QuizAttemptEntity)

    @Insert
    suspend fun insertAll(attempts: List<QuizAttemptEntity>)

    /** Number of attempts for a specific word (for SRS difficulty adjustment). */
    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE userId = :userId AND wordId = :wordId")
    suspend fun getAttemptCountForWord(userId: String, wordId: Int): Int

    /** Success count for a specific word. */
    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE userId = :userId AND wordId = :wordId AND isCorrect = 1")
    suspend fun getSuccessCountForWord(userId: String, wordId: Int): Int

    /** All attempts for today. */
    @Query("SELECT * FROM quiz_attempts WHERE userId = :userId AND dateKey = :dateKey ORDER BY createdAt DESC")
    suspend fun getAttemptsForDate(userId: String, dateKey: String): List<QuizAttemptEntity>

    /** Lookup by dedupe key for exactly-once session commits. */
    @Query("SELECT * FROM quiz_attempts WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getByIdempotencyKey(key: String): QuizAttemptEntity?

    /** Durable events whose projection was never applied (crash recovery). */
    @Query("""
        SELECT * FROM quiz_attempts
        WHERE projectionApplied = 0 AND idempotencyKey IS NOT NULL AND userId = :userId
        ORDER BY createdAt ASC
        LIMIT :limit
    """)
    suspend fun getUnapplied(userId: String, limit: Int = 100): List<QuizAttemptEntity>

    @Query("UPDATE quiz_attempts SET projectionApplied = 1 WHERE idempotencyKey = :key")
    suspend fun markApplied(key: String)

    /** Un-synced attempts for background push. */
    @Query("SELECT * FROM quiz_attempts WHERE synced = 0 ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getUnsyncedAttempts(limit: Int = 100): List<QuizAttemptEntity>

    /** Mark attempts as synced. */
    @Query("UPDATE quiz_attempts SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("UPDATE quiz_attempts SET userId = :toUserId WHERE userId = :fromUserId")
    suspend fun migrateUserId(fromUserId: String, toUserId: String)

    @Query("DELETE FROM quiz_attempts WHERE synced = 1 AND createdAt < :before")
    suspend fun pruneSyncedOlderThan(before: Long)
}
