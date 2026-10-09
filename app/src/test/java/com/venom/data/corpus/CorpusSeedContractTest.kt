package com.venom.data.corpus

import com.venom.domain.model.WordMaster
import com.venom.domain.repo.IWordMasterRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Ticket 02 golden contracts: first launch seeds exactly once; re-launch never duplicates.
 * Hand fakes, real seeder, zero mocks.
 */
class CorpusSeedContractTest {

    private class FakeWordRepo(var count: Int = 0) : IWordMasterRepository {
        val inserted = mutableListOf<WordMaster>()
        override suspend fun getWordById(id: Int): WordMaster? = null
        override suspend fun getWordsByIds(ids: List<Int>): List<WordMaster> = emptyList()
        override suspend fun getWordsByLevel(
            level: com.venom.domain.model.CefrLevel,
            limit: Int
        ): List<WordMaster> = emptyList()
        override suspend fun getWordsByCefr(
            cefrLevel: com.venom.domain.model.CefrLevel,
            limit: Int
        ): List<WordMaster> = emptyList()
        override suspend fun getLevelProgress(level: com.venom.domain.model.WordLevels): Float = 0f
        override suspend fun getLevelsProgress(): Map<String, Float> = emptyMap()
        override suspend fun countWordsByCefr(level: com.venom.domain.model.CefrLevel): Int = 0
    override suspend fun getWordCount(): Int = count
        override suspend fun insertWords(words: List<WordMaster>) {
            inserted += words
            count += words.size
        }
        
        override suspend fun insertWordsOrIgnore(words: List<WordMaster>) {
            val existingIds = inserted.map { it.id }.toSet()
            val newWords = words.filter { it.id !in existingIds }
            inserted += newWords
            count += newWords.size
        }
        override suspend fun getRandomWords(limit: Int): List<WordMaster> = emptyList()
    }

    private class FakeFlagStore(var seeded: Boolean = false, var version: Int = 1) : SeedFlagStore {
        override suspend fun isSeeded(): Boolean = seeded
        override suspend fun setSeeded() {
            seeded = true
        }
        override suspend fun getCorpusVersion(): Int = version
        override suspend fun setCorpusVersion(v: Int) {
            version = v
        }
    }

    private val oneWordJson = """
        {"words": [{
            "id": 1, "wordEn": "substantial", "pos": "adjective",
            "cefrLevel": "B2", "rank": 1500, "frequency": 3,
            "phoneticUs": "səbˈstænʃəl", "phoneticAr": "سَبْسْتانْشَلْ", "translit": "Kabīr",
            "definitionEn": "Large in size or amount.",
            "arabicAr": "كبير", "examples": {"B2": "They made a substantial profit."},
            "wordFamily": {"adj": "substantial", "adv": "substantially"},
            "checklist": ["lemma", "meaning", "definition", "example-form", "collocations", "synonyms", "recall-target", "production-target", "topic-relevance", "sign-off"],
            "collocations": ["substantial increase"], "synonyms": ["considerable"],
            "recallTarget": "They made a ______ profit.",
            "productionExpected": "substantial", "productionVariants": [],
            "sourceRef": "pilot-curation", "ieltsTopic": "Economy",
            "ieltsRelevance": "Frequent in Task 1 reports and Task 2 economy essays.",
            "reviewer": "pilot", "reviewedAt": "2026-09-24"
        }]}
    """.trimIndent()

    @Test
    fun `first launch seeds the store exactly once`() = runTest {
        val repo = FakeWordRepo()
        val flags = FakeFlagStore()
        val seeder = CorpusSeeder(repo, flags)

        val inserted = seeder.seedIfNeeded(oneWordJson)

        assertEquals(1, inserted, "one valid word in, one row seeded")
        assertEquals(1, repo.inserted.size, "exactly one insert call")
        assertTrue(flags.seeded, "seed flag set so re-launch skips")
    }

    @Test
    fun `re-launch never duplicates`() = runTest {
        val repo = FakeWordRepo()
        val flags = FakeFlagStore()
        val seeder = CorpusSeeder(repo, flags)

        seeder.seedIfNeeded(oneWordJson)
        val second = seeder.seedIfNeeded(oneWordJson)

        assertEquals(0, second, "second launch inserts nothing")
        assertEquals(1, repo.inserted.size, "no duplicate rows")
    }

    @Test
    fun `non-empty store without flag still skips`() = runTest {
        val repo = FakeWordRepo(count = 42)
        val flags = FakeFlagStore(seeded = false)
        val seeder = CorpusSeeder(repo, flags)

        assertEquals(0, seeder.seedIfNeeded(oneWordJson), "populated store is never reseeded")
        assertTrue(repo.inserted.isEmpty(), "no writes to a populated store")
    }

    private val expansionBatchJson = """
        {"words": [{
            "id": 1001, "wordEn": "expansion", "pos": "noun",
            "cefrLevel": "B2", "rank": 6001, "frequency": 3,
            "phoneticUs": "ɪkˈspænʃən", "phoneticAr": "إكسبانشن", "translit": "Tawassu",
            "definitionEn": "The action of becoming larger.",
            "arabicAr": "توسع", "examples": {"B2": "The expansion of the company."},
            "wordFamily": {"noun": "expansion"},
            "checklist": ["lemma"],
            "collocations": ["rapid expansion"], "synonyms": ["growth"],
            "recallTarget": "The ______ of the company.",
            "productionExpected": "expansion", "productionVariants": [],
            "sourceRef": "expansion-batch-1", "ieltsTopic": "Economy",
            "ieltsRelevance": "Relevant.",
            "reviewer": "pilot", "reviewedAt": "2026-10-09"
        }]}
    """.trimIndent()

    private val invalidExpansionBatchJson = """
        {"words": [{
            "id": 100, "wordEn": "invalid", "pos": "noun",
            "cefrLevel": "B2", "rank": 6002, "frequency": 3,
            "phoneticUs": "ɪnˈvælɪd", "phoneticAr": "إنفاليد", "translit": "Ghayr",
            "definitionEn": "Not valid.",
            "arabicAr": "غير صالح", "examples": {"B2": "An invalid id."},
            "wordFamily": {"noun": "invalid"},
            "checklist": ["lemma"],
            "collocations": ["invalid id"], "synonyms": ["wrong"],
            "recallTarget": "An ______ id.",
            "productionExpected": "invalid", "productionVariants": [],
            "sourceRef": "expansion-batch-1", "ieltsTopic": "Technology",
            "ieltsRelevance": "Relevant.",
            "reviewer": "pilot", "reviewedAt": "2026-10-09"
        }]}
    """.trimIndent()

    @Test
    fun `applyExpansionBatch validates and inserts words with id greater than or equal to 1001 and updates version`() = runTest {
        val repo = FakeWordRepo()
        val flags = FakeFlagStore(version = 1)
        val seeder = CorpusSeeder(repo, flags)

        val inserted = seeder.applyExpansionBatch(expansionBatchJson, 2)

        assertEquals(1, inserted)
        assertEquals(1, repo.inserted.size)
        assertEquals(1001, repo.inserted.first().id)
        assertEquals(2, flags.version)
    }

    @Test
    fun `applyExpansionBatch skips execution if corpusVersion is greater than or equal to batchVersion`() = runTest {
        val repo = FakeWordRepo()
        val flags = FakeFlagStore(version = 2)
        val seeder = CorpusSeeder(repo, flags)

        val inserted = seeder.applyExpansionBatch(expansionBatchJson, 2)

        assertEquals(0, inserted)
        assertTrue(repo.inserted.isEmpty())
        assertEquals(2, flags.version)
    }

    @Test
    fun `applyExpansionBatch throws CorpusValidationException and rejects the entire batch if any id is less than 1001`() = runTest {
        val repo = FakeWordRepo()
        val flags = FakeFlagStore(version = 1)
        val seeder = CorpusSeeder(repo, flags)

        var exceptionThrown = false
        try {
            seeder.applyExpansionBatch(invalidExpansionBatchJson, 2)
        } catch (e: CorpusValidationException) {
            exceptionThrown = true
            assertTrue(e.message!!.contains("ids < 1001"))
        }

        assertTrue(exceptionThrown)
        assertTrue(repo.inserted.isEmpty())
        assertEquals(1, flags.version)
    }

    @Test
    fun `applyExpansionBatch uses insertWordsOrIgnore semantics avoiding replacements`() = runTest {
        val repo = FakeWordRepo()
        val flags = FakeFlagStore(version = 1)
        val seeder = CorpusSeeder(repo, flags)

        seeder.applyExpansionBatch(expansionBatchJson, 2)
        assertEquals(1, repo.inserted.size)
        
        flags.version = 1
        val duplicateInsert = seeder.applyExpansionBatch(expansionBatchJson, 2)
        
        assertEquals(1, duplicateInsert)
        assertEquals(1, repo.inserted.size, "Duplicate ID was ignored, not appended")
    }

    @Test
    fun `pilot-seeded state doesn't prevent expansion batch application`() = runTest {
        val repo = FakeWordRepo()
        val flags = FakeFlagStore(seeded = true, version = 1)
        val seeder = CorpusSeeder(repo, flags)

        val pilotResult = seeder.seedIfNeeded(oneWordJson)
        assertEquals(0, pilotResult, "Pilot seeding is skipped because seeded=true")

        val expansionResult = seeder.applyExpansionBatch(expansionBatchJson, 2)
        assertEquals(1, expansionResult, "Expansion batch is applied regardless of seeded state")
        assertEquals(2, flags.version)
    }
}
