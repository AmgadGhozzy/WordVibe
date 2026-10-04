package com.venom.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.venom.data.local.entity.XpEventEntity

@Dao
interface XpEventDao {

    @Insert
    suspend fun insert(event: XpEventEntity)

    @Insert
    suspend fun insertAll(events: List<XpEventEntity>)

    /** Today's total XP for a user (for dashboard display). */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_events WHERE userId = :userId AND dateKey = :dateKey")
    suspend fun getTodayXp(userId: String, dateKey: String): Int

    /** Lifetime total XP (for reconciliation against cached profile.totalXp). */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_events WHERE userId = :userId")
    suspend fun getTotalXp(userId: String): Int

    /** Un-synced events for background push. */
    @Query("SELECT * FROM xp_events WHERE synced = 0 ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getUnsyncedEvents(limit: Int = 100): List<XpEventEntity>

    /** Mark events as synced after successful push. */
    @Query("UPDATE xp_events SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("UPDATE xp_events SET userId = :toUserId WHERE userId = :fromUserId")
    suspend fun migrateUserId(fromUserId: String, toUserId: String)

    /** XP earned in a date range (for weekly/monthly reports). */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_events WHERE userId = :userId AND dateKey BETWEEN :from AND :to")
    suspend fun getXpInRange(userId: String, from: String, to: String): Int

    @Query("DELETE FROM xp_events WHERE synced = 1 AND createdAt < :before")
    suspend fun pruneSyncedOlderThan(before: Long)
}
