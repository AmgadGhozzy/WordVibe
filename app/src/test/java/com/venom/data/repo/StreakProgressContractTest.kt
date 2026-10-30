package com.venom.data.repo

import com.venom.analytics.CrashlyticsManager
import com.venom.data.local.dao.UserActivityDao
import com.venom.data.local.dao.UserProfileDao
import com.venom.data.local.dao.UserWordProgressDao
import com.venom.data.local.dao.XpEventDao
import com.venom.data.local.entity.UserActivityEntity
import com.venom.data.local.entity.UserProfileEntity
import com.venom.data.local.entity.UserWordProgressEntity
import com.venom.data.local.entity.XpEventEntity
import com.venom.domain.provider.AppConfigProvider
import com.venom.domain.repo.IRemoteConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Ticket 05 golden contracts: honest streaks at the repository seam.
 * A freeze bridges exactly one missed day while fabricating nothing; unprotected gaps
 * reset to zero; dashboard totals come from SQL aggregates, never in-memory scans.
 */
class StreakProgressContractTest {

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private fun day(offsetDays: Long): String =
        LocalDate.now().plusDays(offsetDays).format(fmt)

    private class FakeActivityDao : UserActivityDao {
        override suspend fun getProductionCountsByDate(userId: String, fromDateKey: String): List<com.venom.data.local.dao.DayCount> = emptyList()
        override suspend fun insertOrIgnore(entity: UserActivityEntity): Long {
            val key = entity.userId to entity.dateKey
            return if (rows.containsKey(key)) -1L else { rows[key] = entity; 1L }
        }
        val rows = mutableMapOf<Pair<String, String>, UserActivityEntity>()
        override suspend fun upsert(entity: UserActivityEntity) {
            rows[entity.userId to entity.dateKey] = entity
        }
        override suspend fun getActivityForDate(
            userId: String,
            dateKey: String
        ): UserActivityEntity? = rows[userId to dateKey]
        // Deliberately empty: dashboard totals must come from the SUM methods below,
        // proving the SQL-aggregate path instead of in-memory scans.
        // Decoy rows: if the repo ever regresses to in-memory sums, totals inflate
        // and this contract fails. Correct totals can only come from the SUM path.
        override suspend fun getAllActivities(userId: String): List<UserActivityEntity> =
            listOf(
                UserActivityEntity(
                    userId = userId, dateKey = "decoy",
                    wordsViewed = 1000, sessionCount = 100, totalTimeMs = 1_000_000
                )
            )
        override suspend fun getRecentActivities(
            userId: String,
            limit: Int
        ): List<UserActivityEntity> = emptyList()
        override suspend fun getActiveDays(userId: String): List<String> =
            rows.keys.filter { it.first == userId }.map { it.second }.sortedDescending()
        override suspend fun getTotalActiveDays(userId: String): Int =
            rows.keys.count { it.first == userId }
        override suspend fun getTotalXp(userId: String): Int? = 0
        override suspend fun getTotalWordsViewed(userId: String): Int =
            rows.values.filter { it.userId == userId }.sumOf { it.wordsViewed }
        override suspend fun getTotalSessionCount(userId: String): Int =
            rows.values.filter { it.userId == userId }.sumOf { it.sessionCount }
        override suspend fun getTotalTimeMs(userId: String): Long =
            rows.values.filter { it.userId == userId }.sumOf { it.totalTimeMs }
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
        override suspend fun addXp(userId: String, dateKey: String, xp: Int, now: Long) {
            val current = rows[userId to dateKey] ?: return
            rows[userId to dateKey] = current.copy(totalXpEarned = current.totalXpEarned + xp)
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
            val current = rows[userId to dateKey] ?: return
            rows[userId to dateKey] =
                current.copy(dailyGoalMet = true, totalXpEarned = current.totalXpEarned + bonusXp)
        }
        override suspend fun migrateUserId(fromUserId: String, toUserId: String) {}
        override suspend fun deleteByUserId(userId: String) {}
    }

    private class FakeProfileDao : UserProfileDao {
        val store = mutableMapOf<String, UserProfileEntity>()
        var freezesConsumed = 0
        override suspend fun upsert(entity: UserProfileEntity) {
            store[entity.userId] = entity
        }
        override suspend fun getProfile(userId: String): UserProfileEntity? = store[userId]
        override suspend fun addXpAtomic(userId: String, xp: Int, now: Long) {
            val current = store[userId] ?: return
            store[userId] = current.copy(totalXp = current.totalXp + xp)
        }
        override suspend fun updateStreak(
            userId: String,
            streak: Int,
            lastActive: String,
            now: Long
        ) {
            val current = store[userId] ?: return
            store[userId] = current.copy(
                currentStreak = streak,
                lastActiveDate = lastActive,
                bestStreak = maxOf(current.bestStreak, streak)
            )
        }
        override suspend fun grantStreakFreeze(userId: String, now: Long) {}
        override suspend fun consumeStreakFreeze(userId: String, now: Long) {
            freezesConsumed++
            val current = store[userId] ?: return
            store[userId] = current.copy(streakFreezes = current.streakFreezes - 1)
        }
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

    private class FakeProgressDao : UserWordProgressDao {
        override suspend fun getProgress(userId: String, wordId: Int): UserWordProgressEntity? =
            null
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
        override suspend fun upsert(progress: UserWordProgressEntity) {}
        override suspend fun upsertAll(progressList: List<UserWordProgressEntity>) {}
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

    private class Harness(
        val activityDao: FakeActivityDao = FakeActivityDao(),
        val profileDao: FakeProfileDao = FakeProfileDao(),
        val xpDao: FakeXpDao = FakeXpDao()
    ) {
        val repo = UserActivityRepository(
            activityDao = activityDao,
            profileDao = profileDao,
            xpEventDao = xpDao,
            progressDao = FakeProgressDao(),
            crashlytics = FakeCrashlytics(),
            appConfigProvider = AppConfigProvider(FakeRemoteConfig())
        )
    }

    @Test
    fun `single missed day with freeze preserves continuity and fabricates nothing`() =
        runTest {
            val h = Harness()
            val dayBeforeYesterday = day(-2)
            h.activityDao.rows["u1" to dayBeforeYesterday] =
                UserActivityEntity(userId = "u1", dateKey = dayBeforeYesterday)
            h.profileDao.store["u1"] = UserProfileEntity(
                userId = "u1", currentStreak = 1, streakFreezes = 1
            )

            h.repo.ensureTodayExists("u1")

            assertEquals(
                3, h.profileDao.store["u1"]!!.currentStreak,
                "bridged chain counts through the covered day"
            )
            assertEquals(1, h.profileDao.freezesConsumed, "exactly one freeze consumed")
            assertEquals(0, h.profileDao.store["u1"]!!.streakFreezes, "inventory decremented")
            assertTrue(
                h.activityDao.rows.keys.none { it == ("u1" to day(-1)) },
                "no activity row is fabricated for the bridged day"
            )
            assertTrue(
                h.xpDao.events.none { it.dateKey == day(-1) },
                "no XP is awarded for the bridged day"
            )
        }

    @Test
    fun `unprotected gap resets to zero without touching freezes`() = runTest {
        val h = Harness()
        h.activityDao.rows["u1" to day(-5)] =
            UserActivityEntity(userId = "u1", dateKey = day(-5))
        h.profileDao.store["u1"] = UserProfileEntity(
            userId = "u1", currentStreak = 9, streakFreezes = 2
        )

        h.repo.ensureTodayExists("u1")

        assertEquals(0, h.profileDao.store["u1"]!!.currentStreak, "unprotected gap resets")
        assertEquals(0, h.profileDao.freezesConsumed, "unbridgeable gap consumes nothing")
        assertEquals(2, h.profileDao.store["u1"]!!.streakFreezes, "inventory untouched")
    }

    @Test
    fun `bridged milestone never mints XP`() = runTest {
        val h = Harness()
        // Five real days ending the day before yesterday + freeze + today = bridged 7.
        listOf(-2, -3, -4, -5, -6).forEach { offset ->
            h.activityDao.rows["u1" to day(offset.toLong())] =
                UserActivityEntity(userId = "u1", dateKey = day(offset.toLong()))
        }
        h.profileDao.store["u1"] = UserProfileEntity(
            userId = "u1", currentStreak = 5, streakFreezes = 1
        )

        h.repo.ensureTodayExists("u1")

        assertEquals(7, h.profileDao.store["u1"]!!.currentStreak, "bridged chain counts to 7")
        assertTrue(
            h.xpDao.events.none { it.source.startsWith("streak_milestone") },
            "a bridged 7 must not mint milestone XP for a day with no study"
        )
    }

    @Test
    fun `dashboard totals come from aggregates`() = runTest {
        val h = Harness()
        h.activityDao.rows["u1" to day(-1)] = UserActivityEntity(
            userId = "u1", dateKey = day(-1),
            wordsViewed = 7, sessionCount = 2, totalTimeMs = 9_000
        )
        h.activityDao.rows["u1" to day(-2)] = UserActivityEntity(
            userId = "u1", dateKey = day(-2),
            wordsViewed = 5, sessionCount = 1, totalTimeMs = 3_000
        )

        val dashboard = h.repo.getDashboardData("u1")

        assertEquals(12, dashboard.totalWordsViewed, "SUM(wordsViewed) across days")
        assertEquals(3, dashboard.totalSessionCount, "SUM(sessionCount) across days")
        assertEquals(12_000L, dashboard.totalTimeMs, "SUM(totalTimeMs) across days")
    }
}
