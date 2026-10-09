package com.venom.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.venom.data.local.entity.WordMasterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordMasterDao {

    @Query("SELECT * FROM wordsMaster ORDER BY rank ASC")
    fun getAllWords(): Flow<List<WordMasterEntity>>

    @Query("SELECT * FROM wordsMaster WHERE id = :id")
    suspend fun getWordById(id: Int): WordMasterEntity?

    @Query("SELECT * FROM wordsMaster WHERE id IN (:ids)")
    suspend fun getWordsByIds(ids: List<Int>): List<WordMasterEntity>

    @Query("SELECT * FROM wordsMaster WHERE cefrLevel = :level ORDER BY rank ASC LIMIT :limit")
    suspend fun getWordsByLevel(level: String, limit: Int = 50): List<WordMasterEntity>

    @Query("SELECT * FROM wordsMaster WHERE cefrLevel = :level ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomWordsByCefr(level: String, limit: Int): List<WordMasterEntity>

    @Query("SELECT COUNT(*) FROM wordsMaster WHERE cefrLevel = :level")
    suspend fun getCountByCefr(level: String): Int

    @Query("SELECT * FROM wordsMaster WHERE unitId = :unitId ORDER BY rank ASC")
    suspend fun getWordsByUnit(unitId: String): List<WordMasterEntity>

    @Query("SELECT DISTINCT unitId FROM wordsMaster WHERE cefrLevel = :level ORDER BY unitId ASC")
    suspend fun getUnitsByLevel(level: String): List<String>

    @Query("SELECT COUNT(*) FROM wordsMaster")
    suspend fun getWordCount(): Int

    /** New words for a user: word rows with no progress row, rank-ordered (SRS order). */
    @Query("""
        SELECT w.* FROM wordsMaster w
        LEFT JOIN userWordProgress p
          ON p.wordId = w.id AND p.userId = :userId
        WHERE p.wordId IS NULL
        ORDER BY w.rank ASC
        LIMIT :limit
    """)
    suspend fun getNewWordsForUser(userId: String, limit: Int): List<WordMasterEntity>

    /** Count of the same frozen new-word definition (LEFT JOIN no-progress-row). */
    @Query("""
        SELECT COUNT(*) FROM wordsMaster w
        LEFT JOIN userWordProgress p
          ON p.wordId = w.id AND p.userId = :userId
        WHERE p.wordId IS NULL
    """)
    suspend fun countNewWordsForUser(userId: String): Int

    /** Get random words (for fallback / topic-based loading). */
    @Query("""
        SELECT * FROM wordsMaster
        ORDER BY RANDOM()
        LIMIT :limit
    """)
    suspend fun getRandomWords(limit: Int = 15): List<WordMasterEntity>

    @Query("""
        SELECT w.* FROM wordsMaster w
        LEFT JOIN userWordProgress p
          ON p.wordId = w.id AND p.userId = :userId
        WHERE p.wordId IS NULL
        ORDER BY RANDOM()
        LIMIT :limit
    """)
    suspend fun getRandomUnseenWordsForUser(userId: String, limit: Int): List<WordMasterEntity>

    @Query("""
        SELECT w.* FROM wordsMaster w
        LEFT JOIN userWordProgress p
          ON p.wordId = w.id AND p.userId = :userId
        WHERE (p.wordId IS NULL OR p.knownState != 'MASTERED')
          AND w.id NOT IN (:excludeIds)
        ORDER BY RANDOM()
        LIMIT :limit
    """)
    suspend fun getRandomNotMasteredWordsForUser(
        userId: String,
        excludeIds: List<Int>,
        limit: Int
    ): List<WordMasterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<WordMasterEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllOrIgnore(words: List<WordMasterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(word: WordMasterEntity)
}
