package com.venom.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.venom.data.local.entity.UsageDailyEntity

@Dao
interface UsageDailyDao {

    @Query("SELECT * FROM usage_daily WHERE userId = :userId AND dateKey = :dateKey")
    suspend fun getUsage(userId: String, dateKey: String): UsageDailyEntity?

    /**
     * Atomic upsert for daily usage.
     * If the row doesn't exist, INSERT with translations=0, aiCalls=0, ocrScans=0.
     * This is idempotent – safe to call on every session start.
     */
    @Query("""
        INSERT OR IGNORE INTO usage_daily (userId, dateKey, translations, aiCalls, ocrScans, updatedAt)
        VALUES (:userId, :dateKey, 0, 0, 0, :now)
    """)
    suspend fun ensureExists(userId: String, dateKey: String, now: Long)

    /** Atomic increment for translations counter. */
    @Query("""
        UPDATE usage_daily 
        SET translations = translations + 1, updatedAt = :now 
        WHERE userId = :userId AND dateKey = :dateKey
    """)
    suspend fun incrementTranslations(userId: String, dateKey: String, now: Long)

    /** Atomic increment for AI calls counter. */
    @Query("""
        UPDATE usage_daily 
        SET aiCalls = aiCalls + 1, updatedAt = :now 
        WHERE userId = :userId AND dateKey = :dateKey
    """)
    suspend fun incrementAiCalls(userId: String, dateKey: String, now: Long)

    /** Atomic increment for OCR scans counter. */
    @Query("""
        UPDATE usage_daily 
        SET ocrScans = ocrScans + 1, updatedAt = :now 
        WHERE userId = :userId AND dateKey = :dateKey
    """)
    suspend fun incrementOcrScans(userId: String, dateKey: String, now: Long)

    @Query("""
        UPDATE usage_daily
        SET userId = :toUserId
        WHERE userId = :fromUserId
          AND NOT EXISTS (
            SELECT 1 FROM usage_daily u2
            WHERE u2.userId = :toUserId AND u2.dateKey = usage_daily.dateKey
          )
    """)
    suspend fun migrateUserId(fromUserId: String, toUserId: String)

    @Query("DELETE FROM usage_daily WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)
}
