package com.venom.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.venom.data.local.WordVibeDatabase
import com.venom.data.local.entity.UserWordProgressEntity
import com.venom.data.local.entity.WordMasterEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WordMasterDaoInstrumentedTest {

    private lateinit var db: WordVibeDatabase
    private lateinit var dao: WordMasterDao

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WordVibeDatabase::class.java
        ).build()
        dao = db.wordMasterDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testNotExistsQueryFiltersOnlyMasteredWords() = runBlocking {
        // Setup: Insert 6 words covering all states and user isolation
        val words = listOf(
            WordMasterEntity(id = 1, wordEn = "word1", cefrLevel = "A1", rank = 1), // No progress for test_user
            WordMasterEntity(id = 2, wordEn = "word2", cefrLevel = "A1", rank = 2), // NEW for test_user
            WordMasterEntity(id = 3, wordEn = "word3", cefrLevel = "A1", rank = 3), // LEARNING for test_user
            WordMasterEntity(id = 4, wordEn = "word4", cefrLevel = "A1", rank = 4), // KNOWN for test_user
            WordMasterEntity(id = 5, wordEn = "word5", cefrLevel = "A1", rank = 5), // MASTERED for test_user (excluded)
            WordMasterEntity(id = 6, wordEn = "word6", cefrLevel = "A1", rank = 6)  // MASTERED for other_user only (must be returned for test_user)
        )
        dao.insertAll(words)

        // Insert progress for user 'test_user'
        db.userWordProgressDao().upsert(
            UserWordProgressEntity(userId = "test_user", wordId = 2, knownState = "NEW", totalAttempts = 1, firstSeen = 1L)
        )
        db.userWordProgressDao().upsert(
            UserWordProgressEntity(userId = "test_user", wordId = 3, knownState = "LEARNING", totalAttempts = 2, firstSeen = 1L)
        )
        db.userWordProgressDao().upsert(
            UserWordProgressEntity(userId = "test_user", wordId = 4, knownState = "KNOWN", totalAttempts = 3, firstSeen = 1L)
        )
        db.userWordProgressDao().upsert(
            UserWordProgressEntity(userId = "test_user", wordId = 5, knownState = "MASTERED", totalAttempts = 4, firstSeen = 1L)
        )

        // Insert progress for 'other_user' with MASTERED for word 6
        db.userWordProgressDao().upsert(
            UserWordProgressEntity(userId = "other_user", wordId = 6, knownState = "MASTERED", totalAttempts = 5, firstSeen = 1L)
        )

        // The query should return words 1, 2, 3, 4, and 6. Word 5 (MASTERED by test_user) MUST be excluded.
        // Limit is 10 (greater than total candidate pool of 5), and results evaluated as a Set to be independent of ORDER BY RANDOM()
        val result = dao.getRandomNotMasteredWordsForUser("test_user", 10)

        // Assertions
        assertEquals("Should return exactly 5 words", 5, result.size)
        val resultIds = result.map { it.id }.toSet()
        assertEquals(setOf(1, 2, 3, 4, 6), resultIds)
    }

    @Test
    fun explainQueryPlanForGetWordsByLevelUsesCefrIndex() {
        val explainCursor = db.query(
            "EXPLAIN QUERY PLAN SELECT * FROM wordsMaster WHERE cefrLevel = 'A1' ORDER BY rank ASC LIMIT 50", null
        )
        val sb = java.lang.StringBuilder()
        while (explainCursor.moveToNext()) {
            val row = mutableListOf<String>()
            for (i in 0 until explainCursor.columnCount) {
                row.add(explainCursor.getString(i) ?: "null")
            }
            sb.append(row.joinToString(" | ")).append("\n")
        }
        explainCursor.close()
        val planStr = sb.toString()
        android.util.Log.i("EXPLAIN_PLAN", "EXPLAIN_QUERY_PLAN_CEFR_LEVEL:\n$planStr")
        println("EXPLAIN_QUERY_PLAN_CEFR_LEVEL:\n$planStr")

        org.junit.Assert.assertTrue(
            "Query plan should use index_wordsMaster_cefrLevel. Plan: $planStr",
            planStr.contains("index_wordsMaster_cefrLevel")
        )
    }

    @Test
    fun explainQueryPlanForGetWordsByRankUsesRankIndex() {
        val explainCursor = db.query(
            "EXPLAIN QUERY PLAN SELECT * FROM wordsMaster ORDER BY rank ASC", null
        )
        val sb = java.lang.StringBuilder()
        while (explainCursor.moveToNext()) {
            val row = mutableListOf<String>()
            for (i in 0 until explainCursor.columnCount) {
                row.add(explainCursor.getString(i) ?: "null")
            }
            sb.append(row.joinToString(" | ")).append("\n")
        }
        explainCursor.close()
        val planStr = sb.toString()
        android.util.Log.i("EXPLAIN_PLAN", "EXPLAIN_QUERY_PLAN_RANK:\n$planStr")
        println("EXPLAIN_QUERY_PLAN_RANK:\n$planStr")

        org.junit.Assert.assertTrue(
            "Query plan should use index_wordsMaster_rank. Plan: $planStr",
            planStr.contains("index_wordsMaster_rank")
        )
    }

    @Test
    fun explainQueryPlanForGetRandomNotMasteredWordsForUser() {
        val explainCursor = db.query(
            "EXPLAIN QUERY PLAN SELECT w.* FROM wordsMaster w " +
            "WHERE NOT EXISTS (" +
            "  SELECT 1 FROM userWordProgress p " +
            "  WHERE p.wordId = w.id AND p.userId = 'test_user' AND p.knownState = 'MASTERED'" +
            ") ORDER BY RANDOM() LIMIT 10", null
        )
        val sb = java.lang.StringBuilder()
        val columnNames = explainCursor.columnNames
        sb.append(columnNames.joinToString(", ")).append("\n")
        while (explainCursor.moveToNext()) {
            val row = mutableListOf<String>()
            for (i in 0 until explainCursor.columnCount) {
                row.add(explainCursor.getString(i) ?: "null")
            }
            sb.append(row.joinToString(" | ")).append("\n")
        }
        explainCursor.close()
        val planStr = sb.toString()
        android.util.Log.i("EXPLAIN_PLAN", "EXPLAIN_QUERY_PLAN_NOT_EXISTS:\n$planStr")

        // Subquery must search via index/primary key, NOT scan the entire userWordProgress table
        org.junit.Assert.assertFalse(
            "Subquery should not perform table scan on userWordProgress. Plan: $planStr",
            planStr.contains("SCAN p") || planStr.contains("SCAN TABLE userWordProgress")
        )
        org.junit.Assert.assertTrue(
            "Subquery should use index search on userWordProgress. Plan: $planStr",
            planStr.contains("SEARCH") && (planStr.contains("p USING") || planStr.contains("userWordProgress USING"))
        )
    }

    @Test
    fun explainQueryPlanForCompositeIndexComparison() {
        val singleCursor = db.query(
            "EXPLAIN QUERY PLAN SELECT * FROM wordsMaster WHERE cefrLevel = 'A1' ORDER BY rank ASC LIMIT 50", null
        )
        val singleSb = java.lang.StringBuilder()
        while (singleCursor.moveToNext()) {
            val row = mutableListOf<String>()
            for (i in 0 until singleCursor.columnCount) {
                row.add(singleCursor.getString(i) ?: "null")
            }
            singleSb.append(row.joinToString(" | ")).append("\n")
        }
        singleCursor.close()
        val singlePlan = singleSb.toString()
        android.util.Log.i("EXPLAIN_PLAN", "SINGLE_INDEX_PLAN:\n$singlePlan")

        // Create composite index and check proposed plan
        db.openHelper.writableDatabase.execSQL("CREATE INDEX IF NOT EXISTS index_wordsMaster_cefr_rank_temp ON wordsMaster(cefrLevel, rank)")
        val compositeCursor = db.query(
            "EXPLAIN QUERY PLAN SELECT * FROM wordsMaster WHERE cefrLevel = 'A1' ORDER BY rank ASC LIMIT 50", null
        )
        val compositeSb = java.lang.StringBuilder()
        while (compositeCursor.moveToNext()) {
            val row = mutableListOf<String>()
            for (i in 0 until compositeCursor.columnCount) {
                row.add(compositeCursor.getString(i) ?: "null")
            }
            compositeSb.append(row.joinToString(" | ")).append("\n")
        }
        compositeCursor.close()
        val compositePlan = compositeSb.toString()
        android.util.Log.i("EXPLAIN_PLAN", "COMPOSITE_INDEX_PLAN:\n$compositePlan")
    }
}
