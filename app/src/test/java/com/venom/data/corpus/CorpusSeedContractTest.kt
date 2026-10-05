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
        override suspend fun getRandomWords(limit: Int): List<WordMaster> = emptyList()
    }

    private class FakeFlagStore(var seeded: Boolean = false) : SeedFlagStore {
        override suspend fun isSeeded(): Boolean = seeded
        override suspend fun setSeeded() {
            seeded = true
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
}
