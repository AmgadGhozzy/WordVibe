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
    fun sqliteVersionLog() {
        val cursor = db.query("SELECT sqlite_version()", null)
        var version = "unknown"
        if (cursor.moveToFirst()) {
            version = cursor.getString(0) ?: "unknown"
        }
        cursor.close()
        android.util.Log.i("EXPLAIN_PLAN", "SQLITE_VERSION: $version")
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val outDir = context.getExternalFilesDir(null) ?: context.filesDir
        java.io.File(outDir, "sqlite_version.txt").writeText(version)
        org.junit.Assert.assertTrue("SQLite version must be present", version.isNotBlank())
    }

    @Test
    fun explainQueryPlanForGetWordsByLevelUsesCompositeIndex() {
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
        android.util.Log.i("EXPLAIN_PLAN", "EXPLAIN_QUERY_PLAN_CEFR_LEVEL_COMPOSITE:\n$planStr")

        org.junit.Assert.assertTrue(
            "Query plan should use index_wordsMaster_cefrLevel_rank. Plan: $planStr",
            planStr.contains("index_wordsMaster_cefrLevel_rank")
        )
        org.junit.Assert.assertFalse(
            "Query plan must not create temporary b-tree when composite index exists. Plan: $planStr",
            planStr.contains("USE TEMP B-TREE")
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

        org.junit.Assert.assertTrue(
            "Query plan should use index_wordsMaster_rank. Plan: $planStr",
            planStr.contains("index_wordsMaster_rank")
        )
    }

    @Test
    fun explainQueryPlanForGetNewWordsForUserUsesRankIndex() {
        val explainCursor = db.query(
            "EXPLAIN QUERY PLAN SELECT w.* FROM wordsMaster w " +
            "LEFT JOIN userWordProgress p ON p.wordId = w.id AND p.userId = 'test_user' " +
            "WHERE p.wordId IS NULL " +
            "ORDER BY w.rank ASC LIMIT 15", null
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
        android.util.Log.i("EXPLAIN_PLAN", "EXPLAIN_QUERY_PLAN_NEW_WORDS:\n$planStr")

        org.junit.Assert.assertTrue(
            "Query plan should scan wordsMaster using index_wordsMaster_rank. Plan: $planStr",
            planStr.contains("index_wordsMaster_rank")
        )
        org.junit.Assert.assertFalse(
            "Query plan must not use temp b-tree for ORDER BY rank. Plan: $planStr",
            planStr.contains("USE TEMP B-TREE")
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
    fun explainQueryPlanRealisticCorpus4800Rows() = runBlocking {
        // Fill 4,800 realistic rows without running ANALYZE
        val levels = listOf("B1", "B2", "C1", "C2")
        val batch = ArrayList<WordMasterEntity>(4800)
        for (i in 1..4800) {
            val level = levels[i % levels.size]
            batch.add(
                WordMasterEntity(
                    id = 100 + i,
                    wordEn = "word_$i",
                    cefrLevel = level,
                    rank = i
                )
            )
        }
        dao.insertAll(batch)

        // 1. getWordsByLevel plan
        val q1Cursor = db.query(
            "EXPLAIN QUERY PLAN SELECT * FROM wordsMaster WHERE cefrLevel = 'B2' ORDER BY rank ASC LIMIT 50", null
        )
        val sb1 = StringBuilder()
        while (q1Cursor.moveToNext()) {
            sb1.append((0 until q1Cursor.columnCount).joinToString(" | ") { q1Cursor.getString(it) ?: "null" }).append("\n")
        }
        q1Cursor.close()
        val q1Plan = sb1.toString()
        android.util.Log.i("REALISTIC_EXPLAIN", "REALISTIC_4800_CEFR_LEVEL_PLAN:\n$q1Plan")
        org.junit.Assert.assertTrue(q1Plan.contains("index_wordsMaster_cefrLevel_rank"))
        org.junit.Assert.assertFalse(q1Plan.contains("USE TEMP B-TREE"))

        // 2. getNewWordsForUser plan
        val q2Cursor = db.query(
            "EXPLAIN QUERY PLAN SELECT w.* FROM wordsMaster w " +
            "LEFT JOIN userWordProgress p ON p.wordId = w.id AND p.userId = 'test_user' " +
            "WHERE p.wordId IS NULL " +
            "ORDER BY w.rank ASC LIMIT 15", null
        )
        val sb2 = StringBuilder()
        while (q2Cursor.moveToNext()) {
            sb2.append((0 until q2Cursor.columnCount).joinToString(" | ") { q2Cursor.getString(it) ?: "null" }).append("\n")
        }
        q2Cursor.close()
        val q2Plan = sb2.toString()
        android.util.Log.i("REALISTIC_EXPLAIN", "REALISTIC_4800_NEW_WORDS_PLAN:\n$q2Plan")
        org.junit.Assert.assertTrue(q2Plan.contains("index_wordsMaster_rank"))
        org.junit.Assert.assertFalse(q2Plan.contains("USE TEMP B-TREE"))

        // 3. getRandomNotMasteredWordsForUser plan
        val q3Cursor = db.query(
            "EXPLAIN QUERY PLAN SELECT w.* FROM wordsMaster w " +
            "WHERE NOT EXISTS (" +
            "  SELECT 1 FROM userWordProgress p " +
            "  WHERE p.wordId = w.id AND p.userId = 'test_user' AND p.knownState = 'MASTERED'" +
            ") ORDER BY RANDOM() LIMIT 10", null
        )
        val sb3 = StringBuilder()
        while (q3Cursor.moveToNext()) {
            sb3.append((0 until q3Cursor.columnCount).joinToString(" | ") { q3Cursor.getString(it) ?: "null" }).append("\n")
        }
        q3Cursor.close()
        val q3Plan = sb3.toString()
        android.util.Log.i("REALISTIC_EXPLAIN", "REALISTIC_4800_NOT_EXISTS_PLAN:\n$q3Plan")
        org.junit.Assert.assertFalse(q3Plan.contains("SCAN p") || q3Plan.contains("SCAN TABLE userWordProgress"))

        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val outDir = context.getExternalFilesDir(null) ?: context.filesDir
        val report = """
            === REALISTIC_4800_CEFR_LEVEL_PLAN ===
            $q1Plan
            === REALISTIC_4800_NEW_WORDS_PLAN ===
            $q2Plan
            === REALISTIC_4800_NOT_EXISTS_PLAN ===
            $q3Plan
        """.trimIndent()
        java.io.File(outDir, "realistic_plans.txt").writeText(report)
    }
}
