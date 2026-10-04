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
import com.venom.domain.model.KnownState
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
 * Ticket 04f: the integrated learning loop, proven end to end through real collaborators:
 * new → due → retrieved → rescheduled → mastered, with the dashboard reconciling exactly
 * against stored events. Functional in-memory fakes (real accumulation, real sums).
 */
class LearningLoopIntegrationTest {

    private class FakeProgressDao : UserWordProgressDao {
        val rows = mutableMapOf<Pair<String, Int>, UserWordProgressEntity>()
        override suspend fun getProgress(userId: String, wordId: Int): UserWordProgressEntity? =
            rows[userId to wordId]
        override fun getAllProgress(userId: String): Flow<List<UserWordProgressEntity>> =
            flowOf(rows.values.filter { it.userId == userId })
        override suspend fun getDueWords(
            userId: String,
            now: Long,
            limit: Int
        ): List<UserWordProgressEntity> = rows.values
            .filter { it.userId == userId && it.nextReview != null && it.nextReview <= now }
            .sortedBy { it.nextReview }
            .take(limit)
        override suspend fun getHighLapseWords(
            userId: String,
            limit: Int
        ): List<UserWordProgressEntity> = emptyList()
        override fun getBookmarkedWords(userId: String): Flow<List<UserWordProgressEntity>> =
            flowOf(emptyList())
        override suspend fun getAllProgressSnapshot(userId: String): List<UserWordProgressEntity> =
            rows.values.filter { it.userId == userId }
        override suspend fun getSeenWordIds(userId: String): List<Int> =
            rows.keys.filter { it.first == userId }.map { it.second }
        override suspend fun migrateUserId(oldUserId: String, newUserId: String) {}
        override suspend fun getProgressCount(userId: String): Int =
            rows.keys.count { it.first == userId }
        override suspend fun upsert(progress: UserWordProgressEntity) {
            rows[progress.userId to progress.wordId] = progress
        }
        override suspend fun upsertAll(progressList: List<UserWordProgressEntity>) {
            progressList.forEach { upsert(it) }
        }
        override suspend fun setBookmarked(userId: String, wordId: Int, bookmarked: Boolean) {}
        override suspend fun getSeenCountByCefr(userId: String, cefrLevel: String): Int = 0
        override suspend fun getMasteredCount(userId: String): Int =
            rows.values.count { it.userId == userId && it.knownState == "MASTERED" }
        override suspend fun getKnownCount(userId: String): Int = 0
        override suspend fun getLearningCount(userId: String): Int =
            rows.values.count {
                it.userId == userId && (it.knownState == "LEARNING" || it.knownState == "KNOWN")
            }
        override suspend fun getNeedsReviewCount(userId: String, now: Long): Int =
            rows.values.count {
                it.userId == userId && it.nextReview != null && it.nextReview <= now
            }
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

    private class FakeWordDao(val words: List<WordMasterEntity>) : WordMasterDao {
        override fun getAllWords(): Flow<List<WordMasterEntity>> = flowOf(words)
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
        override suspend fun getNewWordsForUser(userId: String, limit: Int): List<WordMasterEntity> =
            emptyList()

        // The word fake has no progress visibility; nothing in this suite reads the count.
        override suspend fun countNewWordsForUser(userId: String): Int = 0
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
        val events = mutableListOf<QuizAttemptEntity>()
        override suspend fun insert(attempt: QuizAttemptEntity) {
            if (attempt.idempotencyKey != null &&
                events.any { it.idempotencyKey == attempt.idempotencyKey }
            ) {
                throw IllegalStateException("UNIQUE constraint failed: quiz_attempts.idempotencyKey")
            }
            events += attempt
        }
        override suspend fun insertAll(attempts: List<QuizAttemptEntity>) {
            attempts.forEach { insert(it) }
        }
        override suspend fun getByIdempotencyKey(key: String): QuizAttemptEntity? =
            events.firstOrNull { it.idempotencyKey == key }
        override suspend fun getUnapplied(userId: String, limit: Int): List<QuizAttemptEntity> =
            events.filter {
                !it.projectionApplied && it.idempotencyKey != null && it.userId == userId
            }.take(limit)
        override suspend fun markApplied(key: String) {
            events.replaceAll {
                if (it.idempotencyKey == key) it.copy(projectionApplied = true) else it
            }
        }
        override suspend fun getAttemptCountForWord(userId: String, wordId: Int): Int =
            events.count { it.userId == userId && it.wordId == wordId }
        override suspend fun getSuccessCountForWord(userId: String, wordId: Int): Int =
            events.count { it.userId == userId && it.wordId == wordId && it.isCorrect }
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
        val rows = mutableMapOf<Pair<String, String>, UserActivityEntity>()
        override suspend fun upsert(entity: UserActivityEntity) {
            rows[entity.userId to entity.dateKey] = entity
        }
        override suspend fun getActivityForDate(
            userId: String,
            dateKey: String
        ): UserActivityEntity? = rows[userId to dateKey]
        override suspend fun getAllActivities(userId: String): List<UserActivityEntity> =
            rows.values.filter { it.userId == userId }
        override suspend fun getRecentActivities(
            userId: String,
            limit: Int
        ): List<UserActivityEntity> = emptyList()
        override suspend fun getActiveDays(userId: String): List<String> =
            rows.keys.filter { it.first == userId }.map { it.second }.distinct().sortedDescending()
        override suspend fun getTotalActiveDays(userId: String): Int =
            rows.keys.count { it.first == userId }
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
        ) {
            bump(userId, dateKey) { it.copy(recallSuccess = it.recallSuccess + 1) }
        }
        override suspend fun incrementRecallFail(userId: String, dateKey: String, now: Long) {
            bump(userId, dateKey) { it.copy(recallFail = it.recallFail + 1) }
        }
        override suspend fun incrementPracticeSuccess(
            userId: String,
            dateKey: String,
            xp: Int,
            now: Long
        ) {
            bump(userId, dateKey) { it.copy(productionSuccess = it.productionSuccess + 1) }
        }
        override suspend fun incrementMastered(userId: String, dateKey: String, xp: Int, now: Long) {
            bump(userId, dateKey) { it.copy(masteredCount = it.masteredCount + 1) }
        }
        override suspend fun addXp(userId: String, dateKey: String, xp: Int, now: Long) {
            bump(userId, dateKey) { it.copy(totalXpEarned = it.totalXpEarned + xp) }
        }
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
        ) {
            bump(userId, dateKey) {
                it.copy(dailyGoalMet = true, totalXpEarned = it.totalXpEarned + bonusXp)
            }
        }
        override suspend fun migrateUserId(fromUserId: String, toUserId: String) {}
        override suspend fun deleteByUserId(userId: String) {}
        private suspend fun bump(
            userId: String,
            dateKey: String,
            f: (UserActivityEntity) -> UserActivityEntity
        ) {
            val current = rows[userId to dateKey] ?: return
            rows[userId to dateKey] = f(current)
        }
    }

    private class FakeProfileDao : UserProfileDao {
        val store = mutableMapOf<String, UserProfileEntity>()
        override suspend fun upsert(entity: UserProfileEntity) {
            store[entity.userId] = entity
        }
        override suspend fun getProfile(userId: String): UserProfileEntity? = store[userId]
        override suspend fun addXpAtomic(userId: String, xp: Int, now: Long) {
            val current = store[userId] ?: return
            store[userId] = current.copy(totalXp = current.totalXp + xp)
        }
        override suspend fun updateStreak(userId: String, streak: Int, lastActive: String, now: Long) {
            val current = store[userId] ?: return
            store[userId] = current.copy(
                currentStreak = streak,
                lastActiveDate = lastActive,
                bestStreak = maxOf(current.bestStreak, streak)
            )
        }
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
        val events = mutableListOf<XpEventEntity>()
        override suspend fun insert(event: XpEventEntity) {
            events += event
        }
        override suspend fun insertAll(events: List<XpEventEntity>) {
            this.events += events
        }
        override suspend fun getTodayXp(userId: String, dateKey: String): Int =
            events.filter { it.userId == userId && it.dateKey == dateKey }.sumOf { it.amount }
        override suspend fun getTotalXp(userId: String): Int =
            events.filter { it.userId == userId }.sumOf { it.amount }
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

    private class Harness(
        val progressDao: FakeProgressDao = FakeProgressDao(),
        val wordDao: FakeWordDao = FakeWordDao(
            listOf(WordMasterEntity(id = 7, wordEn = "obtain"))
        ),
        val quizDao: FakeQuizDao = FakeQuizDao(),
        val activityDao: FakeActivityDao = FakeActivityDao(),
        val profileDao: FakeProfileDao = FakeProfileDao(),
        val xpDao: FakeXpDao = FakeXpDao()
    ) {
        private val crashlytics = FakeCrashlytics()
        val activityRepository = UserActivityRepository(
            activityDao = activityDao,
            profileDao = profileDao,
            xpEventDao = xpDao,
            progressDao = progressDao,
            crashlytics = crashlytics,
            appConfigProvider = AppConfigProvider(FakeRemoteConfig())
        )
        val progressRepository = UserWordProgressRepository(
            progressDao = progressDao,
            wordMasterDao = wordDao,
            quizAttemptDao = quizDao,
            srsEngine = SrsEngine(),
            activityRepository = activityRepository,
            crashlytics = crashlytics,
            transactions = FakeTransactions()
        )
    }

    @Test
    fun `a word travels new to mastered through due with reconciled numbers`() = runTest {
        val h = Harness()
        val now = System.currentTimeMillis()

        // New: no progress row anywhere.
        val shown = h.progressRepository.getDisplayProgress("u1", 7)
        assertEquals(KnownState.NEW, shown.knownState, "journey starts at NEW")
        assertTrue(
            h.progressDao.rows.none { it.key == ("u1" to 7) },
            "display creates nothing"
        )

        // Due: one retrieval, then time passes and the word comes due.
        h.progressRepository.recordRecallSuccess("u1", 7, "r0", "s1")
        val lapsed = h.progressDao.rows["u1" to 7]!!
        h.progressDao.rows["u1" to 7] = lapsed.copy(nextReview = now - 1_000)
        assertEquals(
            listOf(7),
            h.progressRepository.getWordsDueForReview("u1", 10).map { it.id },
            "past-due word is served as due"
        )

        // Retrieved to mastery: two more recalls (3/3 recall total) and three productions.
        h.progressRepository.recordRecallSuccess("u1", 7, "r1", "s1")
        h.progressRepository.recordRecallSuccess("u1", 7, "r2", "s1")
        h.progressRepository.recordProductionSuccess("u1", 7, "p1", "s1")
        h.progressRepository.recordProductionSuccess("u1", 7, "p2", "s1")
        h.progressRepository.recordProductionSuccess("u1", 7, "p3", "s1")

        // Mastered and rescheduled out of due.
        val progress = h.progressDao.rows["u1" to 7]!!
        assertEquals(KnownState.MASTERED.name, progress.knownState, "3/3 successes master")
        assertTrue(
            h.progressRepository.getWordsDueForReview("u1", 10).isEmpty(),
            "mastered word reschedules out of due"
        )
        assertTrue(
            (progress.nextReview ?: 0L) > System.currentTimeMillis(),
            "mastered word reschedules into the future"
        )

        // Reconciled against the event log (single journey day, derived from events).
        val wordEvents = h.quizDao.events.filter { it.userId == "u1" && it.wordId == 7 }
        assertEquals(6, wordEvents.size, "six taps, six stored events")
        assertEquals(
            wordEvents.count { it.questionType == "RECALL" && it.isCorrect },
            progress.recallSuccess,
            "recall counter matches its event log"
        )
        assertEquals(
            wordEvents.count { it.questionType == "PRODUCTION" && it.isCorrect },
            progress.productionSuccess,
            "production counter matches its event log"
        )
        assertEquals(
            wordEvents.size, progress.totalAttempts,
            "attempt counter matches its event log"
        )
        val journeyDay = wordEvents.map { it.dateKey }.distinct().single()
        val dashboard = h.activityRepository.getDashboardData("u1")
        assertEquals(
            h.xpDao.events.filter { it.userId == "u1" && it.dateKey == journeyDay }
                .sumOf { it.amount },
            dashboard.todayXp,
            "today XP matches the journey-day event sum"
        )
        assertEquals(
            h.xpDao.events.filter { it.userId == "u1" }.sumOf { it.amount },
            dashboard.totalXp,
            "profile total matches the lifetime event sum"
        )
        assertEquals(1, dashboard.totalWordsMastered, "exactly one mastered word")
        assertEquals(1, dashboard.totalWordsLearned, "learned counts the mastered word")
    }
}
