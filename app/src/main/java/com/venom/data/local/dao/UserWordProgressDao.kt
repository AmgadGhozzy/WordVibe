package com.venom.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.venom.data.local.entity.UserWordProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserWordProgressDao {

    @Query("SELECT * FROM userWordProgress WHERE userId = :userId AND wordId = :wordId")
    suspend fun getProgress(userId: String, wordId: Int): UserWordProgressEntity?

    @Query("SELECT * FROM userWordProgress WHERE userId = :userId")
    fun getAllProgress(userId: String): Flow<List<UserWordProgressEntity>>

    @Query("""
        SELECT * FROM userWordProgress 
        WHERE userId = :userId 
          AND nextReview IS NOT NULL 
          AND nextReview <= :now
        ORDER BY nextReview ASC
        LIMIT :limit
    """)
    suspend fun getDueWords(userId: String, now: Long, limit: Int = 20): List<UserWordProgressEntity>

    @Query("""
        SELECT * FROM userWordProgress 
        WHERE userId = :userId 
          AND lapsesCount > 2 
          AND knownState != 'MASTERED'
        ORDER BY lapsesCount DESC
        LIMIT :limit
    """)
    suspend fun getHighLapseWords(userId: String, limit: Int = 10): List<UserWordProgressEntity>

    @Query("SELECT * FROM userWordProgress WHERE userId = :userId AND bookmarked = 1")
    fun getBookmarkedWords(userId: String): Flow<List<UserWordProgressEntity>>

    @Query("SELECT * FROM userWordProgress WHERE userId = :userId")
    suspend fun getAllProgressSnapshot(userId: String): List<UserWordProgressEntity>

    @Query("SELECT wordId FROM userWordProgress WHERE userId = :userId")
    suspend fun getSeenWordIds(userId: String): List<Int>

    @Query("UPDATE userWordProgress SET userId = :newUserId WHERE userId = :oldUserId")
    suspend fun migrateUserId(oldUserId: String, newUserId: String)

    @Query("SELECT COUNT(*) FROM userWordProgress WHERE userId = :userId")
    suspend fun getProgressCount(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: UserWordProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(progressList: List<UserWordProgressEntity>)

    @Query("UPDATE userWordProgress SET bookmarked = :bookmarked WHERE userId = :userId AND wordId = :wordId")
    suspend fun setBookmarked(userId: String, wordId: Int, bookmarked: Boolean)

    @Query("""
        SELECT COUNT(*) FROM userWordProgress 
        JOIN wordsMaster ON userWordProgress.wordId = wordsMaster.id
        WHERE userWordProgress.userId = :userId 
          AND wordsMaster.cefrLevel = :cefrLevel
    """)
    suspend fun getSeenCountByCefr(userId: String, cefrLevel: String): Int

    @Query("SELECT COUNT(*) FROM userWordProgress WHERE userId = :userId AND knownState = 'MASTERED'")
    suspend fun getMasteredCount(userId: String): Int

    @Query("SELECT COUNT(*) FROM userWordProgress WHERE userId = :userId AND knownState = 'KNOWN'")
    suspend fun getKnownCount(userId: String): Int

    @Query("SELECT COUNT(*) FROM userWordProgress WHERE userId = :userId AND knownState IN ('LEARNING', 'KNOWN')")
    suspend fun getLearningCount(userId: String): Int

    @Query("SELECT COUNT(*) FROM userWordProgress WHERE userId = :userId AND nextReview <= :now")
    suspend fun getNeedsReviewCount(userId: String, now: Long): Int

    @Query("""
        UPDATE userWordProgress 
        SET recallSuccess = recallSuccess + 1, totalAttempts = totalAttempts + 1 
        WHERE userId = :userId AND wordId = :wordId
    """)
    suspend fun incrementRecallSuccess(userId: String, wordId: Int)

    @Query("""
        UPDATE userWordProgress 
        SET productionSuccess = productionSuccess + 1, totalAttempts = totalAttempts + 1 
        WHERE userId = :userId AND wordId = :wordId
    """)
    suspend fun incrementProductionSuccess(userId: String, wordId: Int)

    @Query("""
        UPDATE userWordProgress 
        SET totalAttempts = totalAttempts + 1 
        WHERE userId = :userId AND wordId = :wordId
    """)
    suspend fun incrementTotalAttempts(userId: String, wordId: Int)

    @Query("""
        UPDATE userWordProgress 
        SET stability = :stability, difficulty = :difficulty, 
            knownState = :knownState, lastReview = :lastReview, nextReview = :nextReview,
            lapsesCount = :lapsesCount
        WHERE userId = :userId AND wordId = :wordId
    """)
    suspend fun updateSrsFields(
        userId: String,
        wordId: Int,
        stability: Float,
        difficulty: Int,
        knownState: String,
        lastReview: Long,
        nextReview: Long,
        lapsesCount: Int
    )

    @Query("DELETE FROM userWordProgress WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)
}
