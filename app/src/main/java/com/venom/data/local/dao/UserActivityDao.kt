package com.venom.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.venom.data.local.entity.UserActivityEntity

@Dao
interface UserActivityDao {

    @Upsert
    suspend fun upsert(entity: UserActivityEntity)

    @Query("SELECT * FROM userActivity WHERE userId = :userId AND dateKey = :dateKey")
    suspend fun getActivityForDate(userId: String, dateKey: String): UserActivityEntity?

    @Query("SELECT * FROM userActivity WHERE userId = :userId ORDER BY dateKey DESC")
    suspend fun getAllActivities(userId: String): List<UserActivityEntity>

    @Query("SELECT * FROM userActivity WHERE userId = :userId ORDER BY dateKey DESC LIMIT :limit")
    suspend fun getRecentActivities(userId: String, limit: Int): List<UserActivityEntity>

    @Query("SELECT DISTINCT dateKey FROM userActivity WHERE userId = :userId ORDER BY dateKey DESC")
    suspend fun getActiveDays(userId: String): List<String>

    @Query("SELECT COUNT(DISTINCT dateKey) FROM userActivity WHERE userId = :userId")
    suspend fun getTotalActiveDays(userId: String): Int

    @Query("SELECT SUM(totalXpEarned) FROM userActivity WHERE userId = :userId")
    suspend fun getTotalXp(userId: String): Int?

    @Query("SELECT COALESCE(SUM(wordsViewed), 0) FROM userActivity WHERE userId = :userId")
    suspend fun getTotalWordsViewed(userId: String): Int

    @Query("SELECT COALESCE(SUM(sessionCount), 0) FROM userActivity WHERE userId = :userId")
    suspend fun getTotalSessionCount(userId: String): Int

    @Query("SELECT COALESCE(SUM(totalTimeMs), 0) FROM userActivity WHERE userId = :userId")
    suspend fun getTotalTimeMs(userId: String): Long

    @Query("""
        UPDATE userActivity 
        SET wordsViewed = wordsViewed + 1, updatedAt = :now 
        WHERE userId = :userId AND dateKey = :dateKey
    """)
    suspend fun incrementWordsViewed(userId: String, dateKey: String, now: Long)

    @Query("""
        UPDATE userActivity 
        SET recallSuccess = recallSuccess + 1, totalXpEarned = totalXpEarned + :xp, updatedAt = :now 
        WHERE userId = :userId AND dateKey = :dateKey
    """)
    suspend fun incrementRecallSuccess(userId: String, dateKey: String, xp: Int, now: Long)

    @Query("""
        UPDATE userActivity 
        SET recallFail = recallFail + 1, updatedAt = :now 
        WHERE userId = :userId AND dateKey = :dateKey
    """)
    suspend fun incrementRecallFail(userId: String, dateKey: String, now: Long)

    @Query("""
        UPDATE userActivity 
        SET productionSuccess = productionSuccess + 1, wordsPracticed = wordsPracticed + 1, 
            totalXpEarned = totalXpEarned + :xp, updatedAt = :now 
        WHERE userId = :userId AND dateKey = :dateKey
    """)
    suspend fun incrementPracticeSuccess(userId: String, dateKey: String, xp: Int, now: Long)

    @Query("""
        UPDATE userActivity 
        SET masteredCount = masteredCount + 1, totalXpEarned = totalXpEarned + :xp, updatedAt = :now 
        WHERE userId = :userId AND dateKey = :dateKey
    """)
    suspend fun incrementMastered(userId: String, dateKey: String, xp: Int, now: Long)

    @Query("""
        UPDATE userActivity 
        SET totalXpEarned = totalXpEarned + :xp, updatedAt = :now 
        WHERE userId = :userId AND dateKey = :dateKey
    """)
    suspend fun addXp(userId: String, dateKey: String, xp: Int, now: Long)

    @Query("""
        UPDATE userActivity 
        SET totalTimeMs = totalTimeMs + :durationMs, 
            updatedAt = :now 
        WHERE userId = :userId AND dateKey = :dateKey
    """)
    suspend fun addSessionTime(userId: String, dateKey: String, durationMs: Long, now: Long)

    @Query("""
        UPDATE userActivity 
        SET sessionCount = sessionCount + 1, updatedAt = :now 
        WHERE userId = :userId AND dateKey = :dateKey
    """)
    suspend fun incrementSessionCount(userId: String, dateKey: String, now: Long)

    @Query("""
        UPDATE userActivity 
        SET dailyGoalMet = 1, totalXpEarned = totalXpEarned + :bonusXp, updatedAt = :now 
        WHERE userId = :userId AND dateKey = :dateKey
    """)
    suspend fun markDailyGoalMet(userId: String, dateKey: String, bonusXp: Int, now: Long)

    @Query("UPDATE userActivity SET userId = :toUserId WHERE userId = :fromUserId")
    suspend fun migrateUserId(fromUserId: String, toUserId: String)

    @Query("DELETE FROM userActivity WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)
}