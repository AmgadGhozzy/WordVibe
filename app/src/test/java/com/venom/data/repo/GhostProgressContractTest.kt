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
 * Ticket 01 golden contract (RED): no ghost progress.
 *
 * Frozen contract: recording a failure for a word that does not exist in the word store
 * must never fabricate a progress row. Hand-written fakes, real collaborators, zero mocks.
 *
 * Loud-rejection pinning (P-07 chain): every unknown-word rejection — recall, production,
 * and the replay drain — is reported through CrashlyticsManager, whose debug stub mirrors
 * it to logcat under the WordVibeCrash tag.
 */
class GhostProgressContractTest {

    private class FakeWordMasterDao : WordMasterDao {
        val known = mutableMapOf<Int, WordMasterEntity>()
        override fun getAllWords(): Flow<List<WordMasterEntity>> = flowOf(emptyList())
        override suspend fun getWordById(id: Int): WordMasterEntity? = known[id]
        override suspend fun getWordsByIds(ids: List<Int>): List<WordMasterEntity> = emptyList()
        override suspend fun getWordsByLevel(level: String, limit: Int): List<WordMasterEntity> =
            emptyList()
        override suspend fun getRandomWordsByCefr(level: String, limit: Int): List<WordMasterEntity> =
            emptyList()
        override suspend fun getCountByCefr(level: String): Int = 0
        override suspend fun getWordsByUnit(unitId: String): List<WordMasterEntity> = emptyList()
        override suspend fun getUnitsByLevel(level: String): List<String> = emptyList()
        override suspend fun getWordCount(): Int = 0
        override suspend fun getRandomWords(limit: Int): List<WordMasterEntity> = emptyList()
        override suspend fun getNewWordsForUser(
            userId: String,
            limit: Int
        ): List<WordMasterEntity> = emptyList()

        override suspend fun countNewWordsForUser(userId: String): Int = 0
        override suspend fun getRandomUnseenWordsForUser(
            userId: String,
            limit: Int
        ): List<WordMasterEntity> = emptyList()
        override suspend fun getRandomNotMasteredWordsForUser(
            userId: String,
            limit: Int
        ): List<WordMasterEntity> = emptyList()
        override suspend fun insertAll(words: List<WordMasterEntity>) {}
        override suspend fun insertAllOrIgnore(words: List<WordMasterEntity>) {}

        override suspend fun insert(word: WordMasterEntity) {}
    }

    private class FakeProgressDao : UserWordProgressDao {
        val upserts = mutableListOf<UserWordProgressEntity>()
        private val store = mutableMapOf<Pair<String, Int>, UserWordProgressEntity>()

        fun seed(row: UserWordProgressEntity) {
            store[row.userId to row.wordId] = row
        }

        override suspend fun getProgress(userId: String, wordId: Int): UserWordProgressEntity? =
            store[userId to wordId]
        override fun getAllProgress(userId: String): Flow<List<UserWordProgressEntity>> =
            flowOf(emptyList())
        override suspend fun getDueWords(
            userId: String,
            now: Long,
            limit: Int
        ): List<UserWordProgressEntity> = emptyList()
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
            store[progress.userId to progress.wordId] = progress
        }
        override suspend fun upsertAll(progressList: List<UserWordProgressEntity>) {
            progressList.forEach { upsert(it) }
        }
        override suspend fun setBookmarked(userId: String, wordId: Int, bookmarked: Boolean) {}
        override suspend fun getSeenCountByCefr(userId: String, cefrLevel: String): Int = 0
        override suspend fun getMasteredCount(userId: String): Int = 0
        override suspend fun getLearningCount(userId: String): Int = 0
        override suspend fun getNeedsReviewCount(userId: String, now: Long): Int = 0
        override suspend fun getScheduledReviewTimes(userId: String, fromTime: Long): List<Long> = emptyList()
        override suspend fun incrementRecallSuccess(userId: String, wordId: Int) {}
        override suspend fun getKnownCount(userId: String): Int = 0
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

    private class FakeQuizAttemptDao : QuizAttemptDao {
        override suspend fun getAttemptCountsByDate(userId: String, fromDateKey: String): List<com.venom.data.local.dao.DayCount> = emptyList()
        val inserts = mutableListOf<QuizAttemptEntity>()
        override suspend fun insert(attempt: QuizAttemptEntity) {
            // Mirrors the DB-enforced UNIQUE(idempotencyKey): duplicates explode here too.
            if (attempt.idempotencyKey != null &&
                inserts.any { it.idempotencyKey == attempt.idempotencyKey }
            ) {
                throw IllegalStateException("UNIQUE constraint failed: quiz_attempts.idempotencyKey")
            }
            inserts += attempt
        }
        override suspend fun insertAll(attempts: List<QuizAttemptEntity>) {
            attempts.forEach { insert(it) }
        }

        override suspend fun getProductionAttemptCounts(
            userId: String
        ): List<com.venom.data.local.dao.ProductionAttemptCount> = emptyList()
        override suspend fun getByIdempotencyKey(key: String): QuizAttemptEntity? =
            inserts.firstOrNull { it.idempotencyKey == key }
        override suspend fun getUnapplied(userId: String, limit: Int): List<QuizAttemptEntity> =
            inserts.filter {
                !it.projectionApplied && it.idempotencyKey != null && it.userId == userId
            }.take(limit)
        override suspend fun markApplied(key: String) {
            inserts.replaceAll {
                if (it.idempotencyKey == key) it.copy(projectionApplied = true) else it
            }
        }
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
        override suspend fun getProductionCountsByDate(userId: String, fromDateKey: String): List<com.venom.data.local.dao.DayCount> = emptyList()
        override suspend fun insertOrIgnore(entity: UserActivityEntity): Long = -1L
        private val store = mutableMapOf<Pair<String, String>, UserActivityEntity>()
        override suspend fun upsert(entity: UserActivityEntity) {
            store[entity.userId to entity.dateKey] = entity
        }
        override suspend fun getActivityForDate(
            userId: String,
            dateKey: String
        ): UserActivityEntity? = store[userId to dateKey]
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
        override suspend fun grantStreakFreeze(userId: String, now: Long) {}
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

    private class FakeXpEventDao : XpEventDao {
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
        val nonFatals = mutableListOf<Pair<Throwable, String?>>()
        override fun initialize(enableCollection: Boolean) {}
        override fun logNonFatalException(throwable: Throwable, message: String?) {
            nonFatals += throwable to message
        }
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

    private class RepoWithCrash(
        val repo: UserWordProgressRepository,
        val crashlytics: FakeCrashlytics,
    )

    private fun repositoryWithCrash(
        progressDao: FakeProgressDao,
        wordMasterDao: FakeWordMasterDao = FakeWordMasterDao(),
        quizDao: FakeQuizAttemptDao = FakeQuizAttemptDao()
    ): RepoWithCrash {
        val crashlytics = FakeCrashlytics()
        val activityRepository = UserActivityRepository(
            activityDao = FakeActivityDao(),
            profileDao = FakeProfileDao(),
            xpEventDao = FakeXpEventDao(),
            progressDao = progressDao,
            crashlytics = crashlytics,
            appConfigProvider = AppConfigProvider(FakeRemoteConfig())
        )
        return RepoWithCrash(
            UserWordProgressRepository(
                progressDao = progressDao,
                wordMasterDao = wordMasterDao,
                quizAttemptDao = quizDao,
                srsEngine = SrsEngine(),
                activityRepository = activityRepository,
                crashlytics = crashlytics,
                transactions = FakeTransactions()
            ),
            crashlytics
        )
    }

    private fun repository(
        progressDao: FakeProgressDao,
        wordMasterDao: FakeWordMasterDao = FakeWordMasterDao(),
        quizDao: FakeQuizAttemptDao = FakeQuizAttemptDao()
    ): UserWordProgressRepository = repositoryWithCrash(progressDao, wordMasterDao, quizDao).repo

    @Test
    fun `failed recall for unknown word creates no progress row`() = runTest {
        val progressDao = FakeProgressDao()
        val repo = repository(progressDao)
        val unknownWordId = 999_999

        repo.recordRecallFail("u1", unknownWordId)

        assertTrue(
            progressDao.upserts.none { it.wordId == unknownWordId },
            "a failure for a word outside the store must not fabricate progress"
        )
    }

    @Test
    fun `reapplying the same idempotency key is a no-op`() = runTest {
        val progressDao = FakeProgressDao()
        val wordMasterDao = FakeWordMasterDao()
        wordMasterDao.known[7] = WordMasterEntity(id = 7)
        val quizDao = FakeQuizAttemptDao()
        val repo = repository(progressDao, wordMasterDao, quizDao)
        val key = "session:7:AGAIN:1"

        // Same logical tap applied twice (retry / crash replay): second is dropped.
        repo.recordRecallFail("u1", 7, key)
        val attemptsAfterFirst = quizDao.inserts.count { it.idempotencyKey == key }
        val upsertsAfterFirst = progressDao.upserts.count { it.wordId == 7 }
        repo.recordRecallFail("u1", 7, key)

        assertTrue(
            attemptsAfterFirst == 1 &&
                quizDao.inserts.count { it.idempotencyKey == key } == 1,
            "the same tap applied twice must persist exactly one attempt"
        )
        assertTrue(
            progressDao.upserts.count { it.wordId == 7 } == upsertsAfterFirst,
            "the re-applied key must add no further progress writes"
        )
    }

    @Test
    fun `crashed event replays its projection exactly once`() = runTest {
        val progressDao = FakeProgressDao()
        val wordMasterDao = FakeWordMasterDao()
        wordMasterDao.known[7] = WordMasterEntity(id = 7)
        val quizDao = FakeQuizAttemptDao()
        // Event persisted, process died before projection: applied=false.
        quizDao.inserts += QuizAttemptEntity(
            userId = "u1",
            wordId = 7,
            dateKey = "2026-09-24",
            questionType = "RECALL",
            source = "FLASHCARD",
            isCorrect = false,
            selectedAnswer = "",
            correctAnswer = "",
            idempotencyKey = "session:7:AGAIN:1",
            projectionApplied = false
        )
        val repo = repository(progressDao, wordMasterDao, quizDao)

        repo.replayUnappliedEvents("u1")
        val projected = progressDao.upserts.filter { it.wordId == 7 }
        val last = projected.last()

        assertTrue(
            last.lapsesCount == 1 && last.totalAttempts == 1,
            "replay must apply the projection (lapses=1, attempts=1)"
        )
        val writesAfterFirst = projected.size
        repo.replayUnappliedEvents("u1")

        assertTrue(
            progressDao.upserts.count { it.wordId == 7 } == writesAfterFirst,
            "re-running replay must add no further projection writes"
        )
    }

    @Test
    fun `applied events are never reprojected`() = runTest {        val progressDao = FakeProgressDao()
        val wordMasterDao = FakeWordMasterDao()
        wordMasterDao.known[7] = WordMasterEntity(id = 7)
        val quizDao = FakeQuizAttemptDao()
        quizDao.inserts += QuizAttemptEntity(
            userId = "u1",
            wordId = 7,
            dateKey = "2026-09-24",
            questionType = "RECALL",
            source = "FLASHCARD",
            isCorrect = true,
            selectedAnswer = "",
            correctAnswer = "",
            idempotencyKey = "session:7:GOOD:1",
            projectionApplied = true
        )
        val repo = repository(progressDao, wordMasterDao, quizDao)

        repo.replayUnappliedEvents("u1")

        assertTrue(
            progressDao.upserts.none { it.wordId == 7 },
            "an applied event must produce zero projection writes on replay"
        )
    }

    @Test
    fun `production failure for unknown word creates no progress row`() = runTest {
        val progressDao = FakeProgressDao()
        val repo = repository(progressDao)

        repo.recordProductionFail("u1", 999_999, "session:9:PROD_FAIL:1", "s1")

        assertTrue(
            progressDao.upserts.none { it.wordId == 999_999 },
            "a production failure outside the store must not fabricate progress"
        )
    }

    @Test
    fun `production failure moves state the failing direction and dedupes`() = runTest {
        val progressDao = FakeProgressDao()
        val wordMasterDao = FakeWordMasterDao()
        wordMasterDao.known[7] = WordMasterEntity(id = 7)
        val quizDao = FakeQuizAttemptDao()
        val repo = repository(progressDao, wordMasterDao, quizDao)
        val key = "session:7:PROD_FAIL:1"

        repo.recordProductionFail("u1", 7, key, "s1")
        val first = progressDao.upserts.filter { it.wordId == 7 }
        repo.recordProductionFail("u1", 7, key, "s1")

        assertTrue(
            first.isNotEmpty() && first.last().lapsesCount == 1 &&
                first.last().totalAttempts == 1 &&
                first.last().difficulty == 6 &&
                first.last().nextReview != null,
            "production failure must lapse, count, harden, and schedule"
        )
        assertTrue(
            quizDao.inserts.count { it.idempotencyKey == key } == 1 &&
                progressDao.upserts.count { it.wordId == 7 } == first.size,
            "re-applied production failure must add no further writes"
        )
    }

    @Test
    fun `production failure demotes mastered word to known`() = runTest {        val progressDao = FakeProgressDao()
        val wordMasterDao = FakeWordMasterDao()
        wordMasterDao.known[7] = WordMasterEntity(id = 7)
        val repo = repository(progressDao, wordMasterDao)
        progressDao.seed(
            UserWordProgressEntity(
                userId = "u1",
                wordId = 7,
                productionSuccess = 3,
                recallSuccess = 3,
                totalAttempts = 6,
                knownState = "MASTERED",
                difficulty = 3,
                stability = 3.0f
            )
        )

        repo.recordProductionFail("u1", 7, "session:7:PROD_FAIL:1", "s1")

        assertEquals(
            "KNOWN",
            progressDao.upserts.last { it.wordId == 7 }.knownState,
            "any real failure demotes mastery while preserving history"
        )
    }

    @Test
    fun `production success for unknown word creates no progress row`() = runTest {
        val progressDao = FakeProgressDao()
        val repo = repository(progressDao)

        repo.recordProductionSuccess("u1", 999_999, "session:9:PROD:1", "s1")

        assertTrue(
            progressDao.upserts.none { it.wordId == 999_999 },
            "a production success outside the store must not fabricate progress"
        )
    }

    @Test
    fun `production success promotes production and dedupes`() = runTest {
        val progressDao = FakeProgressDao()
        val wordMasterDao = FakeWordMasterDao()
        wordMasterDao.known[7] = WordMasterEntity(id = 7)
        val quizDao = FakeQuizAttemptDao()
        val repo = repository(progressDao, wordMasterDao, quizDao)
        val key = "session:7:PROD:1"

        repo.recordProductionSuccess("u1", 7, key, "s1")
        val first = progressDao.upserts.filter { it.wordId == 7 }
        repo.recordProductionSuccess("u1", 7, key, "s1")

        assertTrue(
            first.isNotEmpty() && first.last().productionSuccess == 1 &&
                first.last().recallSuccess == 0,
            "production success must promote production counters, never recall"
        )
        assertTrue(
            quizDao.inserts.count { it.idempotencyKey == key } == 1 &&
                progressDao.upserts.count { it.wordId == 7 } == first.size,
            "re-applied production success must add no further writes"
        )
    }

    @Test
    fun `replayed production event projects through production path`() = runTest {
        val progressDao = FakeProgressDao()
        val wordMasterDao = FakeWordMasterDao()
        wordMasterDao.known[7] = WordMasterEntity(id = 7)
        val quizDao = FakeQuizAttemptDao()
        quizDao.inserts += QuizAttemptEntity(
            userId = "u1",
            wordId = 7,
            dateKey = "2026-09-24",
            questionType = "PRODUCTION",
            source = "FLASHCARD",
            isCorrect = false,
            selectedAnswer = "",
            correctAnswer = "",
            idempotencyKey = "session:7:PROD_FAIL:1",
            projectionApplied = false
        )
        val repo = repository(progressDao, wordMasterDao, quizDao)

        repo.replayUnappliedEvents("u1")

        val last = progressDao.upserts.last { it.wordId == 7 }
        assertTrue(
            last.lapsesCount == 1 && last.recallSuccess == 0,
            "replayed production failure must lapse without touching recall counters"
        )
    }

    @Test
    fun `recall rejections for unknown words are loud through CrashlyticsManager`() = runTest {
        val progressDao = FakeProgressDao()
        val quizDao = FakeQuizAttemptDao()
        val built = repositoryWithCrash(progressDao, quizDao = quizDao)

        built.repo.recordRecallSuccess("u1", 999_999)
        built.repo.recordRecallFail("u1", 999_999)

        assertEquals(
            2, built.crashlytics.nonFatals.size,
            "both recall signals for a missing word must be reported loudly"
        )
        assertTrue(
            built.crashlytics.nonFatals.all {
                it.second == "Rejected recall event: word missing from store"
            },
            "each rejection carries the loud context message"
        )
        assertTrue(
            built.crashlytics.nonFatals.any {
                it.first.message?.contains("recall success for unknown word 999999") == true
            } && built.crashlytics.nonFatals.any {
                it.first.message?.contains("recall failure for unknown word 999999") == true
            },
            "the exception names the signal and the missing word id"
        )
        assertTrue(
            quizDao.inserts.isEmpty(),
            "no durable event may be persisted for an unknown word"
        )
        assertTrue(
            progressDao.upserts.none { it.wordId == 999_999 },
            "an unknown word must not fabricate progress either"
        )
    }

    @Test
    fun `production rejections for unknown words are loud through CrashlyticsManager`() = runTest {
        val progressDao = FakeProgressDao()
        val quizDao = FakeQuizAttemptDao()
        val built = repositoryWithCrash(progressDao, quizDao = quizDao)

        built.repo.recordProductionFail("u1", 999_999, "session:9:PROD_FAIL:1", "s1")

        assertEquals(
            1, built.crashlytics.nonFatals.size,
            "the production rejection must be reported loudly"
        )
        assertTrue(
            built.crashlytics.nonFatals.single()
                .first.message?.contains("production failure for unknown word 999999") == true,
            "the exception names the signal and the missing word id"
        )
        assertTrue(
            quizDao.inserts.isEmpty(),
            "no durable event may be persisted for an unknown word"
        )
        assertTrue(
            progressDao.upserts.none { it.wordId == 999_999 },
            "an unknown word must not fabricate progress either"
        )
    }

    @Test
    fun `unprojectable replayed events drain loudly through CrashlyticsManager`() = runTest {
        val progressDao = FakeProgressDao()
        val quizDao = FakeQuizAttemptDao()
        quizDao.inserts += QuizAttemptEntity(
            userId = "u1",
            wordId = 999_999,
            dateKey = "2026-10-05",
            questionType = "RECALL",
            source = "FLASHCARD",
            isCorrect = false,
            selectedAnswer = "",
            correctAnswer = "",
            idempotencyKey = "session:999:GHOST:1",
            projectionApplied = false
        )
        val built = repositoryWithCrash(progressDao, quizDao = quizDao)

        built.repo.replayUnappliedEvents("u1")

        assertEquals(
            1, built.crashlytics.nonFatals.size,
            "draining an unprojectable event must be loud"
        )
        assertEquals(
            "Draining unprojectable event",
            built.crashlytics.nonFatals.single().second
        )
        assertTrue(
            quizDao.inserts.single { it.idempotencyKey == "session:999:GHOST:1" }.projectionApplied,
            "the drained event is marked applied exactly once"
        )
        assertTrue(
            progressDao.upserts.isEmpty(),
            "no projection is possible for a missing word"
        )
    }
}
