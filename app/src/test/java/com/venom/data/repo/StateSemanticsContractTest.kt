package com.venom.data.repo

import com.venom.analytics.CrashlyticsManager
import com.venom.data.local.TransactionRunner
import com.venom.data.local.dao.QuizAttemptDao
import com.venom.data.local.dao.UserActivityDao
import com.venom.data.local.dao.UserProfileDao
import com.venom.data.local.dao.UserWordProgressDao
import com.venom.data.local.dao.WordMasterDao
import com.venom.data.local.dao.XpEventDao
import com.venom.data.local.entity.QuizAttemptEntity
import com.venom.data.local.entity.UserActivityEntity
import com.venom.data.local.entity.UserProfileEntity
import com.venom.data.local.entity.UserWordProgressEntity
import com.venom.data.local.entity.WordMasterEntity
import com.venom.data.local.entity.XpEventEntity
import com.venom.domain.model.CefrLevel
import com.venom.domain.model.KnownState
import com.venom.domain.model.UserWordProgress
import com.venom.domain.provider.AppConfigProvider
import com.venom.domain.repo.IRemoteConfig
import com.venom.domain.srs.SrsEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Ticket 04a golden contracts (RED): state semantics live below the UI layer.
 * - Display loads never fabricate progress rows (prefetch is read-only).
 * - New words come from a single user-scoped, DB-backed definition (never ID lists).
 * Hand fakes, real collaborators, zero mocks.
 */
class StateSemanticsContractTest {

    private class FakeProgressDao : UserWordProgressDao {
        val upserts = mutableListOf<UserWordProgressEntity>()
        var rows = mutableMapOf<Pair<String, Int>, UserWordProgressEntity>()
        override suspend fun getProgress(userId: String, wordId: Int): UserWordProgressEntity? =
            rows[userId to wordId]
        override fun getAllProgress(userId: String): Flow<List<UserWordProgressEntity>> =
            flowOf(emptyList())
        override suspend fun getDueWords(
            userId: String,
            now: Long,
            limit: Int
        ): List<UserWordProgressEntity> =
            rows.values.filter { row ->
                row.userId == userId && row.nextReview != null && row.nextReview <= now
            }.sortedBy { it.nextReview }.take(limit)
        override suspend fun getHighLapseWords(
            userId: String,
            limit: Int
        ): List<UserWordProgressEntity> = emptyList()
        override fun getBookmarkedWords(userId: String): Flow<List<UserWordProgressEntity>> =
            flowOf(emptyList())
        override suspend fun getAllProgressSnapshot(userId: String): List<UserWordProgressEntity> =
            emptyList()
        override suspend fun getSeenWordIds(userId: String): List<Int> = emptyList()
        override suspend fun migrateUserId(oldUserId: String, newUserId: String) {}
        override suspend fun getProgressCount(userId: String): Int = 0
        override suspend fun upsert(progress: UserWordProgressEntity) {
            upserts += progress
            rows[progress.userId to progress.wordId] = progress
        }
        override suspend fun upsertAll(progressList: List<UserWordProgressEntity>) {
            progressList.forEach { upsert(it) }
        }
        override suspend fun setBookmarked(userId: String, wordId: Int, bookmarked: Boolean) {}
        override suspend fun getSeenCountByCefr(userId: String, cefrLevel: String): Int = 0
        override suspend fun getMasteredCount(userId: String): Int = 0
        override suspend fun getLearningCount(userId: String): Int = 0
        override suspend fun getNeedsReviewCount(userId: String, now: Long): Int = 0
        override suspend fun incrementRecallSuccess(userId: String, wordId: Int) {}
        override suspend fun incrementProductionSuccess(userId: String, wordId: Int) {}
        override suspend fun incrementTotalAttempts(userId: String, wordId: Int) {}
        override suspend fun updateSrsFields(
            userId: String,
            wordId: Int,
            stability: Float,
            difficulty: Int,
            knownState: String,
            lastReview: Long,
            nextReview: Long,
            lapsesCount: Int
        ) {}
        override suspend fun deleteByUserId(userId: String) {}
    }

    private class FakeWordDao(
        val words: List<WordMasterEntity> = emptyList(),
        val joinResult: List<WordMasterEntity> = emptyList()
    ) : WordMasterDao {
        var joinUserId: String? = null
        var joinLimit: Int = -1
        override fun getAllWords(): Flow<List<WordMasterEntity>> = flowOf(emptyList())
        override suspend fun getWordById(id: Int): WordMasterEntity? =
            words.firstOrNull { it.id == id }
        override suspend fun getWordsByIds(ids: List<Int>): List<WordMasterEntity> =
            words.filter { it.id in ids }
        override suspend fun getWordsByLevel(level: String, limit: Int): List<WordMasterEntity> =
            emptyList()
        override suspend fun getRandomWordsByCefr(level: String, limit: Int): List<WordMasterEntity> =
            emptyList()
        override suspend fun getCountByCefr(level: String): Int = 0
        override suspend fun getWordsByUnit(unitId: String): List<WordMasterEntity> = emptyList()
        override suspend fun getUnitsByLevel(level: String): List<String> = emptyList()
        override suspend fun getWordCount(): Int = words.size
        override suspend fun getRandomWords(limit: Int): List<WordMasterEntity> = emptyList()
        override suspend fun getNewWordsForUser(userId: String, limit: Int): List<WordMasterEntity> {
            joinUserId = userId
            joinLimit = limit
            return joinResult
        }
        override suspend fun getRandomUnseenWordsForUser(
            userId: String,
            limit: Int
        ): List<WordMasterEntity> = emptyList()
        override suspend fun getRandomNotMasteredWordsForUser(
            userId: String,
            excludeIds: List<Int>,
            limit: Int
        ): List<WordMasterEntity> = emptyList()
        override suspend fun insertAll(words: List<WordMasterEntity>) {}
        override suspend fun insert(word: WordMasterEntity) {}
    }

    private class FakeQuizDao : QuizAttemptDao {
        override suspend fun insert(attempt: QuizAttemptEntity) {}
        override suspend fun insertAll(attempts: List<QuizAttemptEntity>) {}
        override suspend fun getByIdempotencyKey(key: String): QuizAttemptEntity? = null
        override suspend fun getUnapplied(userId: String, limit: Int): List<QuizAttemptEntity> =
            emptyList()
        override suspend fun markApplied(key: String) {}
        override suspend fun getAttemptCountForWord(userId: String, wordId: Int): Int = 0
        override suspend fun getSuccessCountForWord(userId: String, wordId: Int): Int = 0
        override suspend fun getAttemptsForDate(
            userId: String,
            dateKey: String
        ): List<QuizAttemptEntity> = emptyList()
        override suspend fun getUnsyncedAttempts(limit: Int): List<QuizAttemptEntity> = emptyList()
        override suspend fun markSynced(ids: List<Long>) {}
        override suspend fun migrateUserId(fromUserId: String, toUserId: String) {}
        override suspend fun pruneSyncedOlderThan(before: Long) {}
    }

    private class FakeActivityDao : UserActivityDao {
        override suspend fun upsert(entity: UserActivityEntity) {}
        override suspend fun getActivityForDate(
            userId: String,
            dateKey: String
        ): UserActivityEntity? = null
        override suspend fun getAllActivities(userId: String): List<UserActivityEntity> =
            emptyList()
        override suspend fun getRecentActivities(
            userId: String,
            limit: Int
        ): List<UserActivityEntity> = emptyList()
        override suspend fun getActiveDays(userId: String): List<String> = emptyList()
        override suspend fun getTotalActiveDays(userId: String): Int = 0
        override suspend fun getTotalXp(userId: String): Int? = 0
        override suspend fun getTotalWordsViewed(userId: String): Int = 0
        override suspend fun getTotalSessionCount(userId: String): Int = 0
        override suspend fun getTotalTimeMs(userId: String): Long = 0L
        override suspend fun incrementWordsViewed(userId: String, dateKey: String, now: Long) {}
        override suspend fun incrementRecallSuccess(
            userId: String,
            dateKey: String,
            xp: Int,
            now: Long
        ) {}
        override suspend fun incrementRecallFail(userId: String, dateKey: String, now: Long) {}
        override suspend fun incrementPracticeSuccess(
            userId: String,
            dateKey: String,
            xp: Int,
            now: Long
        ) {}
        override suspend fun incrementMastered(userId: String, dateKey: String, xp: Int, now: Long) {}
        override suspend fun addXp(userId: String, dateKey: String, xp: Int, now: Long) {}
        override suspend fun addSessionTime(
            userId: String,
            dateKey: String,
            durationMs: Long,
            now: Long
        ) {}
        override suspend fun incrementSessionCount(userId: String, dateKey: String, now: Long) {}
        override suspend fun markDailyGoalMet(
            userId: String,
            dateKey: String,
            bonusXp: Int,
            now: Long
        ) {}
        override suspend fun migrateUserId(fromUserId: String, toUserId: String) {}
        override suspend fun deleteByUserId(userId: String) {}
    }

    private class FakeProfileDao : UserProfileDao {
        private val store = mutableMapOf<String, UserProfileEntity>()
        override suspend fun upsert(entity: UserProfileEntity) {
            store[entity.userId] = entity
        }
        override suspend fun getProfile(userId: String): UserProfileEntity? = store[userId]
        override suspend fun addXpAtomic(userId: String, xp: Int, now: Long) {}
        override suspend fun updateStreak(userId: String, streak: Int, lastActive: String, now: Long) {}
        override suspend fun consumeStreakFreeze(userId: String, now: Long) {}
        override suspend fun updateDailyGoal(userId: String, goal: Int, now: Long) {}
        override suspend fun migrateUserId(fromUserId: String, toUserId: String) {}
        override suspend fun updatePlacementAndXp(
            userId: String,
            unit: Int,
            cefr: String,
            assignedXp: Int,
            takenAt: Long,
            now: Long
        ) {}
        override suspend fun getPlacementUnit(userId: String): Int? = null
        override suspend fun deleteByUserId(userId: String) {}
    }

    private class FakeXpDao : XpEventDao {
        override suspend fun insert(event: XpEventEntity) {}
        override suspend fun insertAll(events: List<XpEventEntity>) {}
        override suspend fun getTodayXp(userId: String, dateKey: String): Int = 0
        override suspend fun getTotalXp(userId: String): Int = 0
        override suspend fun getUnsyncedEvents(limit: Int): List<XpEventEntity> = emptyList()
        override suspend fun markSynced(ids: List<Long>) {}
        override suspend fun migrateUserId(fromUserId: String, toUserId: String) {}
        override suspend fun getXpInRange(userId: String, from: String, to: String): Int = 0
        override suspend fun pruneSyncedOlderThan(before: Long) {}
    }

    private class FakeRemoteConfig : IRemoteConfig {
        override suspend fun fetchAndActivate() {}
        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = defaultValue
        override fun getLong(key: String, defaultValue: Long): Long = defaultValue
        override fun getDouble(key: String, defaultValue: Double): Double = defaultValue
        override fun getString(key: String, defaultValue: String): String = defaultValue
    }

    private class FakeCrashlytics : CrashlyticsManager {
        override fun initialize(enableCollection: Boolean) {}
        override fun logNonFatalException(throwable: Throwable, message: String?) {}
        override fun logBreadcrumb(message: String) {}
        override fun setCustomKey(key: String, value: String) {}
        override fun setCustomKey(key: String, value: Boolean) {}
        override fun setCustomKey(key: String, value: Int) {}
        override fun setCustomKey(key: String, value: Long) {}
        override fun setCustomKey(key: String, value: Float) {}
        override fun setCustomKey(key: String, value: Double) {}
        override fun setCurrentScreen(screenName: String) {}
        override fun setCurrentFeature(featureName: String) {}
        override fun setUserId(userId: String?) {}
        override fun setCrashlyticsCollectionEnabled(enabled: Boolean) {}
        override suspend fun deleteUnsentReports() {}
        override suspend fun hasUnsentReports(): Boolean = false
        override suspend fun sendUnsentReports() {}
    }

    private class FakeTransactions : TransactionRunner {
        override suspend fun <T> withTransaction(block: suspend () -> T): T = block()
    }

    private fun progressRepo(
        progressDao: FakeProgressDao,
        wordDao: FakeWordDao = FakeWordDao()
    ): UserWordProgressRepository {
        val crashlytics = FakeCrashlytics()
        val activityRepository = UserActivityRepository(
            activityDao = FakeActivityDao(),
            profileDao = FakeProfileDao(),
            xpEventDao = FakeXpDao(),
            progressDao = progressDao,
            crashlytics = crashlytics,
            appConfigProvider = AppConfigProvider(FakeRemoteConfig())
        )
        return UserWordProgressRepository(
            progressDao = progressDao,
            wordMasterDao = wordDao,
            quizAttemptDao = FakeQuizDao(),
            srsEngine = SrsEngine(),
            activityRepository = activityRepository,
            crashlytics = crashlytics,
            transactions = FakeTransactions()
        )
    }

    @Test
    fun `display load creates no progress rows`() = runTest {
        val progressDao = FakeProgressDao()
        val repo = progressRepo(progressDao)

        val shown = repo.getDisplayProgress("u1", 7)

        assertTrue(
            progressDao.upserts.isEmpty(),
            "reading a word for display must not fabricate progress"
        )
        assertEquals(KnownState.NEW, shown.knownState, "undisplayed word reads NEW")
        assertEquals(7, shown.wordId, "display progress addresses the requested word")
    }

    @Test
    fun `new words come from the user-scoped join`() = runTest {
        val first = WordMasterEntity(id = 9, wordEn = "consume")
        val second = WordMasterEntity(id = 3, wordEn = "evaluate")
        val wordDao = FakeWordDao(joinResult = listOf(first, second))
        val progressRepo = progressRepo(FakeProgressDao(), wordDao)

        val words = progressRepo.getNewWordsForUser("u1", 10)

        assertEquals("u1", wordDao.joinUserId, "new-word query is scoped to the user")
        assertEquals(10, wordDao.joinLimit, "limit passes through")
        assertEquals(
            listOf(9, 3),
            words.map { it.id },
            "join rows map to domain words preserving DAO order"
        )
    }

    @Test
    fun `viewing writes no progress rows`() = runTest {
        val progressDao = FakeProgressDao()
        val repo = progressRepo(progressDao)

        repo.recordView("u1", 7)

        assertTrue(
            progressDao.upserts.isEmpty(),
            "viewing must not fabricate progress: viewed words stay new until tapped"
        )
    }

    @Test
    fun `due means past review timestamp and nothing else`() = runTest {
        val now = System.currentTimeMillis()
        val progressDao = FakeProgressDao()
        val wordDao = FakeWordDao(
            words = listOf(WordMasterEntity(id = 9, wordEn = "consume"))
        )
        val repo = progressRepo(progressDao, wordDao)
        progressDao.rows["u1" to 9] = UserWordProgressEntity(
            userId = "u1", wordId = 9, nextReview = now - 1_000
        )
        progressDao.rows["u1" to 10] = UserWordProgressEntity(
            userId = "u1", wordId = 10, nextReview = now + 100_000
        )
        progressDao.rows["u1" to 11] = UserWordProgressEntity(
            userId = "u1", wordId = 11, nextReview = null
        )
        progressDao.rows["u1" to 12] = UserWordProgressEntity(
            userId = "u1", wordId = 12, nextReview = now - 2_000
        )

        val due = repo.getWordsDueForReview("u1", 10)

        assertEquals(
            listOf(9),
            due.map { it.id },
            "only past-due rows with resolvable words are due: future, null, and " +
                "missing-word rows are excluded"
        )
    }

    @Test
    fun `state transitions follow the frozen order`() {
        val engine = SrsEngine()

        assertEquals(
            KnownState.SEEN,
            engine.updateKnownState(UserWordProgress(userId = "u", wordId = 1)),
            "encountered with zero attempts reads SEEN"
        )
        assertEquals(
            KnownState.LEARNING,
            engine.updateKnownState(
                UserWordProgress(userId = "u", wordId = 1, totalAttempts = 1)
            ),
            "attempted without success reads LEARNING"
        )
        assertEquals(
            KnownState.KNOWN,
            engine.updateKnownState(
                UserWordProgress(userId = "u", wordId = 1, recallSuccess = 1, totalAttempts = 1)
            ),
            "first success reads KNOWN"
        )
    }
}
