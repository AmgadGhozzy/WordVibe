package com.venom.data.corpus

import com.venom.domain.model.WordMaster
import com.venom.domain.repo.IWordMasterRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Ticket 17 wiring tests (red-first). Reads the real shipped assets and drives the
 * real CorpusSeeder against hand fakes copied from CorpusSeedContractTest (unmodified).
 *
 * NOTE: "Application.onCreate calls applyExpansionBatch" cannot be asserted as a plain
 * JVM unit test — Application/Assets/DataStore need the Android instrumentation runtime,
 * so that assertion is deliberately out of scope here.
 */
class ExpansionWiringTest {

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

    private fun pilotAsset(): String {
        val file = File("src/main/assets/words_pilot.json")
        assertTrue(file.exists(), "pilot asset must exist at src/main/assets/words_pilot.json")
        return file.readText()
    }

    private fun expansionAsset(): String {
        val file = File("src/main/assets/words_expansion_v2.json")
        assertTrue(file.exists(), "expansion asset must exist at src/main/assets/words_expansion_v2.json")
        return file.readText()
    }

    @Test
    fun `T1 expansion asset passes contract with 81 words, ids above 1000, unique, rank at least 6000`() {
        val json = expansionAsset()

        val violations = validateCorpus(json)
        assertTrue(
            violations.isEmpty(),
            "corpus violations:\n" + violations.take(10).joinToString("\n")
        )

        val words = parseCorpus(json)
        assertEquals(81, words.size, "expansion batch must carry exactly 81 words")
        assertTrue(words.all { it.id >= 1001 }, "every expansion id must be >= 1001")
        assertEquals(words.size, words.map { it.id }.toSet().size, "ids must be unique")
        assertTrue(words.none { it.rank < 6000 }, "no expansion rank may be < 6000")
    }

    @Test
    fun `T2 pilot then expansion yields 181 rows in the repo and corpusVersion 2`() = runTest {
        val repo = FakeWordRepo()
        val flags = FakeFlagStore()
        val seeder = CorpusSeeder(repo, flags)

        val pilot = seeder.seedIfNeeded(pilotAsset())
        val expansion = seeder.applyExpansionBatch(expansionAsset(), 2)

        assertEquals(100, pilot)
        assertEquals(81, expansion)
        assertEquals(181, repo.getWordCount(), "row count must be measured from the repository")
        assertEquals(2, flags.version)
    }

    @Test
    fun `T3 re-applying the expansion is a no-op`() = runTest {
        val repo = FakeWordRepo()
        val flags = FakeFlagStore()
        val seeder = CorpusSeeder(repo, flags)
        seeder.seedIfNeeded(pilotAsset())
        seeder.applyExpansionBatch(expansionAsset(), 2)
        val afterFirst = repo.getWordCount()

        seeder.applyExpansionBatch(expansionAsset(), 2)

        assertEquals(afterFirst, repo.getWordCount(), "row count must not change on re-apply")
        assertEquals(2, flags.version)
    }

    @Test
    fun `T4 pilot ids and ranks are unchanged after the expansion`() = runTest {
        val repo = FakeWordRepo()
        val flags = FakeFlagStore()
        val seeder = CorpusSeeder(repo, flags)
        seeder.seedIfNeeded(pilotAsset())
        val before = repo.inserted.map { it.id to it.rank }.sortedBy { it.first }

        seeder.applyExpansionBatch(expansionAsset(), 2)

        val after = repo.inserted.filter { it.id in 1..100 }.map { it.id to it.rank }.sortedBy { it.first }
        assertEquals(before, after, "pilot ids/ranks must be identical after expansion")
    }

    private val corruptExpansionJson = """
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
    fun `E1 bootstrap seeds and expands an empty table to 181 at version 2`() = runTest {
        val repo = FakeWordRepo()
        val flags = FakeFlagStore()
        val seeder = CorpusSeeder(repo, flags)

        ExpansionBootstrap.seedAndExpand(seeder, pilotAsset(), expansionAsset())

        assertEquals(181, repo.getWordCount(), "row count must be measured from the repository")
        assertEquals(2, flags.version)
    }

    @Test
    fun `E2 bootstrap upgrades an existing pilot user to 181 at version 2`() = runTest {
        val repo = FakeWordRepo(count = 100)
        val flags = FakeFlagStore(seeded = true, version = 1)
        val seeder = CorpusSeeder(repo, flags)

        ExpansionBootstrap.seedAndExpand(seeder, pilotAsset(), expansionAsset())

        assertEquals(181, repo.getWordCount())
        assertEquals(2, flags.version)
    }

    @Test
    fun `E3 a second bootstrap call does not change rows`() = runTest {
        val repo = FakeWordRepo()
        val flags = FakeFlagStore()
        val seeder = CorpusSeeder(repo, flags)
        ExpansionBootstrap.seedAndExpand(seeder, pilotAsset(), expansionAsset())
        val afterFirst = repo.getWordCount()

        ExpansionBootstrap.seedAndExpand(seeder, pilotAsset(), expansionAsset())

        assertEquals(afterFirst, repo.getWordCount(), "row count must not change on re-apply")
        assertEquals(2, flags.version)
    }

    @Test
    fun `E4 corrupt expansion throws and leaves rows and version untouched`() = runTest {
        val repo = FakeWordRepo(count = 100)
        val flags = FakeFlagStore(seeded = true, version = 1)
        val seeder = CorpusSeeder(repo, flags)
        val rowsBefore = repo.getWordCount()
        val versionBefore = flags.version

        var threw = false
        try {
            ExpansionBootstrap.seedAndExpand(seeder, pilotAsset(), corruptExpansionJson)
        } catch (e: CorpusValidationException) {
            threw = true
        }

        assertTrue(threw, "a batch with an id < 1001 must be rejected")
        assertEquals(rowsBefore, repo.getWordCount(), "rows must be untouched on rejection")
        assertEquals(versionBefore, flags.version, "version must be untouched on rejection")
    }
}
