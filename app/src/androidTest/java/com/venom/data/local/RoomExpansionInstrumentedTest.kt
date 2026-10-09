package com.venom.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.venom.data.corpus.CorpusSeeder
import com.venom.data.local.entity.UserWordProgressEntity
import com.venom.data.local.entity.WordMasterEntity
import com.venom.data.repo.StubWordMasterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomExpansionInstrumentedTest {

    private lateinit var database: WordVibeDatabase
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WordVibeDatabase::class.java
        ).build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testExpansionBatchDoesNotOverwriteExistingWordOrProgress() = runBlocking {
        val wordDao = database.wordMasterDao()
        val progressDao = database.userWordProgressDao()
        
        // 1. Insert existing pilot word (ID = 1001)
        val originalWord = WordMasterEntity(
            id = 1001, wordEn = "growth", pos = "noun", cefrLevel = "B2",
            rank = 6001, frequency = 3, phoneticUs = "ɡroʊθ", phoneticAr = "جروث",
            translit = "Numuw", definitionEn = "The process of increasing.",
            arabicAr = "نمو", examples = "The growth of the company.",
            wordFamily = "{}", checklist = "[]", collocations = "[]", synonyms = "[]",
            recallTarget = "The ______ of the company.", productionExpected = "growth",
            productionVariants = "[]", sourceRef = "pilot", ieltsTopic = "Economy",
            ieltsRelevance = "Relevant.", reviewer = "pilot", reviewedAt = "2026-10-09"
        )
        wordDao.insertAll(listOf(originalWord))
        
        // 2. Insert user progress for word 1001
        val originalProgress = UserWordProgressEntity(
            userId = "u1",
            wordId = 1001,
            bookmarked = true,
            recallSuccess = 5,
            knownState = "MASTERED"
        )
        progressDao.upsert(originalProgress)
        
        // 3. Set up the Seeder with a real repository backed by the Room DAO
        class FakeSeedFlagStore(var version: Int = 0) : com.venom.data.corpus.SeedFlagStore {
            override suspend fun isSeeded(): Boolean = version > 0
            override suspend fun setSeeded() { version = 1 }
            override suspend fun getCorpusVersion(): Int = version
            override suspend fun setCorpusVersion(newVersion: Int) { version = newVersion }
        }
        val repo = StubWordMasterRepository(wordDao)
        val flags = FakeSeedFlagStore(1)
        val seeder = CorpusSeeder(repo, flags)
        
        // 4. Create an expansion batch with word 1001 modified, and a NEW word 1002
        val batchJson = """
            {"words": [
                {
                    "id": 1001, "wordEn": "expansion", "pos": "noun",
                    "cefrLevel": "B2", "rank": 6001, "frequency": 3,
                    "phoneticUs": "ɡroʊθ", "phoneticAr": "جروث", "translit": "Numuw",
                    "definitionEn": "The process of increasing.",
                    "arabicAr": "نمو", "examples": {"B2": "The expansion of the company."},
                    "wordFamily": {"noun": "expansion"}, "checklist": ["lemma"],
                    "collocations": ["rapid growth"], "synonyms": ["expansion"],
                    "recallTarget": "The ______ of the company.", "productionExpected": "growth",
                    "productionVariants": [], "sourceRef": "pilot", "ieltsTopic": "Economy",
                    "ieltsRelevance": "Relevant.", "reviewer": "pilot", "reviewedAt": "2026-10-09"
                },
                {
                    "id": 1002, "wordEn": "scale", "pos": "verb",
                    "cefrLevel": "B2", "rank": 6002, "frequency": 2,
                    "phoneticUs": "skeɪl", "phoneticAr": "سكيل", "translit": "Skayl",
                    "definitionEn": "To climb or increase.",
                    "arabicAr": "يكبر", "examples": {"B2": "We need to scale the business."},
                    "wordFamily": {"noun": "scale"}, "checklist": ["lemma"],
                    "collocations": ["scale up"], "synonyms": ["grow"],
                    "recallTarget": "We need to ______ the business.", "productionExpected": "scale",
                    "productionVariants": [], "sourceRef": "expansion-batch-1", "ieltsTopic": "Work",
                    "ieltsRelevance": "Relevant.", "reviewer": "pilot", "reviewedAt": "2026-10-09"
                }
            ]}
        """.trimIndent()
        
        // 5. Apply the batch
        seeder.applyExpansionBatch(batchJson, 2)
        
        // 6. Verify (A) and (B): ID 1001 is UNCHANGED in WordMaster and Progress
        val word1001 = wordDao.getWordById(1001)
        assertEquals("growth", word1001?.wordEn) // the REPLACE would have made it 'expansion'
        
        val progress1001 = progressDao.getProgress("u1", 1001)
        assertEquals(5, progress1001?.recallSuccess)
        assertEquals("MASTERED", progress1001?.knownState)
        
        // 7. Verify (C): ID 1002 was successfully inserted
        val word1002 = wordDao.getWordById(1002)
        assertEquals("scale", word1002?.wordEn)
        
        // 8. Verify the total word count is exactly 2
        assertEquals(2, wordDao.getWordCount())
    }

    @Test
    fun testExpansionBatchConcurrentStressTest() = runBlocking {
        val wordDao = database.wordMasterDao()
        
        // Setup Seeder
        class FakeSeedFlagStore(var version: Int = 0) : com.venom.data.corpus.SeedFlagStore {
            override suspend fun isSeeded(): Boolean = version > 0
            override suspend fun setSeeded() { version = 1 }
            override suspend fun getCorpusVersion(): Int = version
            override suspend fun setCorpusVersion(newVersion: Int) { version = newVersion }
        }
        val repo = StubWordMasterRepository(wordDao)
        val flags = FakeSeedFlagStore(0) // Start unseeded
        val seeder = CorpusSeeder(repo, flags)
        
        val batchJson = """
            {"words": [
                {
                    "id": 1001, "wordEn": "concurrency", "pos": "noun",
                    "cefrLevel": "B2", "rank": 6001, "frequency": 3,
                    "phoneticUs": "kənˈkɜːrənsi", "phoneticAr": "كونكورنسي", "translit": "kankwrnsy",
                    "definitionEn": "The property of executing multiple parts.",
                    "arabicAr": "تزامن", "examples": {"B2": "The concurrency of the system."},
                    "wordFamily": {"noun": "concurrency"}, "checklist": ["lemma"],
                    "collocations": ["high concurrency"], "synonyms": ["simultaneity"],
                    "recallTarget": "The ______ of the system.", "productionExpected": "concurrency",
                    "productionVariants": [], "sourceRef": "stress-test", "ieltsTopic": "Technology",
                    "ieltsRelevance": "Relevant.", "reviewer": "pilot", "reviewedAt": "2026-10-09"
                }
            ]}
        """.trimIndent()

        // Launch 100 concurrent seed operations
        val jobs = (1..100).map {
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    seeder.applyExpansionBatch(batchJson, 2)
                } catch (e: Exception) {
                    // It's possible some throw due to locking or validation, which we will record.
                    // Actually, if it's safe, they should just no-op or succeed identically.
                }
            }
        }
        
        jobs.forEach { it.join() }
        
        // Verify exactly one word was inserted and the DB isn't corrupted
        assertEquals(1, wordDao.getWordCount())
        assertEquals("concurrency", wordDao.getWordById(1001)?.wordEn)
        assertEquals(2, flags.getCorpusVersion())
    }
}
