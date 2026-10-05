package com.venom.data.repo

import com.venom.analytics.StubCrashlyticsManager
import com.venom.data.local.entity.UserWordProgressEntity
import com.venom.data.local.entity.WordMasterEntity
import com.venom.domain.provider.AppConfigProvider
import com.venom.domain.srs.SrsEngine
import com.venom.harness.FakeActivityDao
import com.venom.harness.FakeProfileDao
import com.venom.harness.FakeProgressDao
import com.venom.harness.FakeQuizAttemptDao
import com.venom.harness.FakeRemoteConfig
import com.venom.harness.FakeTransactions
import com.venom.harness.FakeWordDao
import com.venom.harness.FakeXpEventDao
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Extra ADR-0010 serving-order contracts over the approved caller-passed
 * `preferredCefr` shape: the placed band comes first (rank-ordered), then the
 * rest (rank-ordered); null keeps pure rank order. Ordering only — the frozen
 * no-progress-row definition and its counter are untouched.
 */
class NewWordsBandOrderingContractTest {

    private fun word(id: Int, cefr: String) = WordMasterEntity(
        id = id,
        wordEn = "word$id",
        cefrLevel = cefr,
        rank = id,
        definitionEn = "definition $id",
        arabicAr = "معنى $id"
    )

    private class Rig(words: List<WordMasterEntity>) {
        val progressDao = FakeProgressDao()
        val wordDao = FakeWordDao(words, progressDao)
        val repo: UserWordProgressRepository

        init {
            val activityRepository = UserActivityRepository(
                activityDao = FakeActivityDao(),
                profileDao = FakeProfileDao(),
                xpEventDao = FakeXpEventDao(),
                progressDao = progressDao,
                crashlytics = StubCrashlyticsManager(),
                appConfigProvider = AppConfigProvider(FakeRemoteConfig())
            )
            repo = UserWordProgressRepository(
                progressDao = progressDao,
                wordMasterDao = wordDao,
                quizAttemptDao = FakeQuizAttemptDao(),
                srsEngine = SrsEngine(),
                activityRepository = activityRepository,
                crashlytics = StubCrashlyticsManager(),
                transactions = FakeTransactions()
            )
        }
    }

    /** A1, A2, B1, B2, B2, C1, C2 — ranks follow ids. */
    private fun corpus() = listOf(
        word(1, "A1"), word(2, "A2"), word(3, "B1"),
        word(4, "B2"), word(7, "B2"), word(5, "C1"), word(6, "C2")
    )

    @Test
    fun `the placed band leads, rank-ordered, then the rest rank-ordered`() = runTest {
        val rig = Rig(corpus())

        val ids = rig.repo.getNewWordsForUser("u1", limit = 10, preferredCefr = "B2").map { it.id }

        assertEquals(
            listOf(4, 7, 1, 2, 3, 5, 6),
            ids,
            "both B2 words by rank first, then everything else by rank"
        )
    }

    @Test
    fun `no preferred band keeps pure rank order`() = runTest {
        val rig = Rig(corpus())

        val ids = rig.repo.getNewWordsForUser("u1", limit = 10, preferredCefr = null).map { it.id }

        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), ids, "rank ASC exactly as before ADR-0010")
    }

    @Test
    fun `a word with a progress row is still excluded from the ordered query`() = runTest {
        val rig = Rig(corpus())
        rig.progressDao.rows["u1" to 4] = UserWordProgressEntity(userId = "u1", wordId = 4)

        val ids = rig.repo.getNewWordsForUser("u1", limit = 10, preferredCefr = "B2").map { it.id }

        assertEquals(listOf(7, 1, 2, 3, 5, 6), ids, "the frozen definition is untouched")
    }

    @Test
    fun `ordering never filters — served count matches the frozen counter`() = runTest {
        val rig = Rig(corpus())

        val words = rig.repo.getNewWordsForUser("u1", limit = 10, preferredCefr = "B2")

        assertEquals(
            rig.wordDao.countNewWordsForUser("u1"),
            words.size,
            "serving order and the new-word counter agree on the population"
        )
    }
}
