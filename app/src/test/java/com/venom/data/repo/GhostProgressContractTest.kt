package com.venom.data.repo

import com.venom.analytics.CrashlyticsManager
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Ticket 01 golden contract (RED): no ghost progress.
 *
 * Frozen contract: recording a failure for a word that does not exist in the word store
 * must never fabricate a progress row. Hand-written fakes, real collaborators, zero mocks.
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
        override suspend fun getNewWords(seenIds: List<Int>, limit: Int): List<WordMasterEntity> =
            emptyList()
        override suspend fun getRandomWords(limit: Int): List<WordMasterEntity> = emptyList()
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

    private class FakeProgressDao : UserWordProgressDao {
        val upserts = mutableListOf<UserWordProgressEntity>()
        private val store = mutableMapOf<Pair<String, Int>, UserWordProgressEntity>()

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

    private class FakeQuizAttemptDao : QuizAttemptDao {
        val inserts = mutableListOf<QuizAttemptEntity>()
        override suspend fun insert(attempt: QuizAttemptEntity) {
            inserts += attempt
        }
        override suspend fun insertAll(attempts: List<QuizAttemptEntity>) {
            inserts += attempts
        }
        override suspend fun getByIdempotencyKey(key: String): QuizAttemptEntity? =
            inserts.firstOrNull { it.idempotencyKey == key }
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

    private fun repository(
        progressDao: FakeProgressDao,
        wordMasterDao: FakeWordMasterDao = FakeWordMasterDao(),
        quizDao: FakeQuizAttemptDao = FakeQuizAttemptDao()
    ): UserWordProgressRepository {
        val crashlytics = FakeCrashlytics()
        val activityRepository = UserActivityRepository(
            activityDao = FakeActivityDao(),
            profileDao = FakeProfileDao(),
            xpEventDao = FakeXpEventDao(),
            progressDao = progressDao,
            crashlytics = crashlytics,
            appConfigProvider = AppConfigProvider(FakeRemoteConfig())
        )
        return UserWordProgressRepository(
            progressDao = progressDao,
            wordMasterDao = wordMasterDao,
            quizAttemptDao = quizDao,
            srsEngine = SrsEngine(),
            activityRepository = activityRepository,
            crashlytics = crashlytics
        )
    }

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
}
