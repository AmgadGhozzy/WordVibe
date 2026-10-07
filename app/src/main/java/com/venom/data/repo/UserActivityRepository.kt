package com.venom.data.repo

import com.venom.analytics.CrashlyticsManager
import com.venom.data.local.DateProvider
import com.venom.data.local.SystemDateProvider
import com.venom.data.local.dao.UserActivityDao
import com.venom.data.local.dao.UserProfileDao
import com.venom.data.local.dao.UserWordProgressDao
import com.venom.data.local.dao.XpEventDao
import com.venom.data.local.entity.UserActivityEntity
import com.venom.data.local.entity.UserProfileEntity
import com.venom.data.local.entity.XpEventEntity
import com.venom.domain.model.DashboardData
import com.venom.domain.model.StreakEngine
import com.venom.domain.model.UserLevel
import com.venom.domain.model.XpResult
import com.venom.domain.provider.AppConfigProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserActivityRepository @Inject constructor(
    private val activityDao: UserActivityDao,
    private val profileDao: UserProfileDao,
    private val xpEventDao: XpEventDao,
    private val progressDao: UserWordProgressDao,
    private val crashlytics: CrashlyticsManager,
    private val appConfigProvider: AppConfigProvider,
    private val dates: DateProvider = SystemDateProvider()
) {
    companion object {
        private const val TAG = "UserActivityRepo"
    }

    fun today(): String = dates.todayKey()

    private fun yesterday(): String = dates.yesterdayKey()

    private fun previousDay(dateKey: String): String = dates.previousDayKey(dateKey)

    suspend fun getOrCreateProfile(userId: String): UserProfileEntity =
        withContext(Dispatchers.IO) {
            profileDao.getProfile(userId) ?: UserProfileEntity(
                userId = userId,
                createdAt = System.currentTimeMillis()
            ).also { profileDao.upsert(it) }
        }

    /**
     * Ticket 14: idempotent under concurrency. The day row is created with
     * INSERT OR IGNORE (atomic on the composite (userId, dateKey) key), and
     * `updateStreakOnNewDay` runs ONLY in the coroutine that actually created
     * the row — a second concurrent caller's insert is ignored (rowId = -1),
     * so milestone/first-session XP can no longer be doubled by the
     * consumer-vs-finalizeSession interleaving.
     */
    suspend fun ensureTodayExists(userId: String) {
        val todayKey = today()
        if (activityDao.getActivityForDate(userId, todayKey) == null) {
            val profile = getOrCreateProfile(userId)
            val created = activityDao.insertOrIgnore(
                UserActivityEntity(
                    userId = userId,
                    dateKey = todayKey,
                    dailyGoalTarget = profile.dailyGoalXp
                )
            )
            if (created != -1L) {
                updateStreakOnNewDay(userId)
            }
        }
    }

    private suspend fun awardXp(
        userId: String,
        baseXp: Int,
        source: String,
        wordId: Int? = null
    ): XpResult = withContext(Dispatchers.IO) {
        val todayKey = today()
        val now = System.currentTimeMillis()
        val profile = getOrCreateProfile(userId)

        val multiplier = (1f + (profile.currentStreak * appConfigProvider.xpStreakMultiplierStep))
            .coerceIn(1f, appConfigProvider.xpStreakMultiplierMax)
        val totalXp = (baseXp * multiplier).toInt()

        xpEventDao.insert(
            XpEventEntity(
                userId = userId,
                dateKey = todayKey,
                source = source,
                baseXp = baseXp,
                streakMultiplier = multiplier,
                amount = totalXp,
                wordId = wordId
            )
        )

        activityDao.addXp(userId, todayKey, totalXp, now)
        profileDao.addXpAtomic(userId, totalXp, now)

        val newTotalXp = profile.totalXp + totalXp
        val oldLevel = UserLevel.fromXp(profile.totalXp)
        val newLevel = UserLevel.fromXp(newTotalXp)
        val leveledUp = newLevel.ordinal > oldLevel.ordinal

        val todayActivity = activityDao.getActivityForDate(userId, todayKey)
        val dailyGoalJustMet = todayActivity != null &&
                !todayActivity.dailyGoalMet &&
                todayActivity.totalXpEarned >= todayActivity.dailyGoalTarget

        if (dailyGoalJustMet) {
            val bonus = appConfigProvider.xpDailyGoalBonus
            activityDao.markDailyGoalMet(userId, todayKey, bonus, now)
            xpEventDao.insert(
                XpEventEntity(
                    userId = userId,
                    dateKey = todayKey,
                    source = "daily_goal_bonus",
                    baseXp = bonus,
                    streakMultiplier = 1f,
                    amount = bonus
                )
            )
            profileDao.addXpAtomic(userId, bonus, now)
        }

        XpResult(
            baseXp = baseXp,
            streakMultiplier = multiplier,
            totalXp = totalXp,
            newTotalXp = newTotalXp,
            newLevel = newLevel,
            leveledUp = leveledUp,
            dailyGoalJustMet = dailyGoalJustMet
        )
    }

    suspend fun recordWordView(userId: String, wordId: Int? = null): XpResult =
        withContext(Dispatchers.IO) {
            ensureTodayExists(userId)
            activityDao.incrementWordsViewed(userId, today(), System.currentTimeMillis())
            awardXp(userId, appConfigProvider.xpWordView, "word_view", wordId)
        }

    suspend fun recordNewWord(userId: String) = withContext(Dispatchers.IO) {
        getOrCreateProfile(userId)
    }

    suspend fun recordRecallSuccess(userId: String, wordId: Int? = null): XpResult =
        withContext(Dispatchers.IO) {
            ensureTodayExists(userId)
            activityDao.incrementRecallSuccess(userId, today(), 0, System.currentTimeMillis())
            awardXp(userId, appConfigProvider.xpRecallSuccess, "recall_success", wordId)
        }

    suspend fun recordRecallFail(userId: String) = withContext(Dispatchers.IO) {
        ensureTodayExists(userId)
        activityDao.incrementRecallFail(userId, today(), System.currentTimeMillis())
    }

    suspend fun recordPracticeSuccess(userId: String, wordId: Int? = null): XpResult =
        withContext(Dispatchers.IO) {
            ensureTodayExists(userId)
            activityDao.incrementPracticeSuccess(userId, today(), 0, System.currentTimeMillis())
            awardXp(userId, appConfigProvider.xpPracticeSuccess, "practice_success", wordId)
        }

    suspend fun recordWordMastered(userId: String, wordId: Int? = null): XpResult =
        withContext(Dispatchers.IO) {
            ensureTodayExists(userId)
            activityDao.incrementMastered(userId, today(), 0, System.currentTimeMillis())
            awardXp(userId, appConfigProvider.xpWordMastered, "word_mastered", wordId)
        }

    suspend fun recordSessionComplete(userId: String, durationMs: Long) =
        withContext(Dispatchers.IO) {
            ensureTodayExists(userId)
            val now = System.currentTimeMillis()
            activityDao.incrementSessionCount(userId, today(), now)
            activityDao.addSessionTime(userId, today(), durationMs, now)
        }

    suspend fun addSessionTime(userId: String, durationMs: Long) =
        withContext(Dispatchers.IO) {
            ensureTodayExists(userId)
            activityDao.addSessionTime(userId, today(), durationMs, System.currentTimeMillis())
        }

    private suspend fun updateStreakOnNewDay(userId: String) {
        val activeDays = activityDao.getActiveDays(userId)
        val todayKey = today()
        val yesterdayKey = yesterday()
        val now = System.currentTimeMillis()

        val profile = getOrCreateProfile(userId)

        // A freeze bridges exactly one missed day. The missed day is synthesized into the
        // counted chain only — never written as activity, XP, or history (continuity-only).
        // priorDays excludes today (just created above): the gap check sees real history.
        val priorDays = activeDays.filter { it != todayKey }
        val bridged = StreakEngine.shouldConsumeFreeze(
            priorDays, todayKey, yesterdayKey, ::previousDay
        ) && profile.streakFreezes > 0
        if (bridged) {
            profileDao.consumeStreakFreeze(userId, now)
        }

        val updatedDays = when {
            bridged -> listOf(todayKey, yesterdayKey) + priorDays
            activeDays.firstOrNull() != todayKey -> listOf(todayKey) + activeDays
            else -> activeDays
        }

        val streak = StreakEngine.calculateStreak(
            updatedDays, todayKey, yesterdayKey, ::previousDay
        )

        profileDao.updateStreak(userId, streak, todayKey, now)

        // Milestones reward real continuity only: a bridged count must never mint
        // milestone XP for a day with no study behind it. The same honesty gates
        // the freeze grant (ADR-0010): earned at exact 7-day marks, capped at 2,
        // never from a bridged day. The pre-update profile streak is the
        // last-granted marker, so a multiple cannot be granted twice.
        if (!bridged) {
            val grant = StreakEngine.freezeGrant(streak, profile.streakFreezes, profile.currentStreak)
            if (grant > profile.streakFreezes) {
                profileDao.grantStreakFreeze(userId, now)
            }
            StreakEngine.checkMilestone(streak)?.let { milestone ->
                xpEventDao.insert(
                    XpEventEntity(
                        userId = userId,
                        dateKey = todayKey,
                        source = "streak_milestone_${milestone.days}",
                        baseXp = milestone.bonusXp,
                        streakMultiplier = 1f,
                        amount = milestone.bonusXp
                    )
                )
                activityDao.addXp(userId, todayKey, milestone.bonusXp, now)
                profileDao.addXpAtomic(userId, milestone.bonusXp, now)
            }
        }

        if (profile.lastActiveDate != todayKey) {
            val bonus = appConfigProvider.xpFirstSessionBonus
            xpEventDao.insert(
                XpEventEntity(
                    userId = userId,
                    dateKey = todayKey,
                    source = "first_session_bonus",
                    baseXp = bonus,
                    streakMultiplier = 1f,
                    amount = bonus
                )
            )
            activityDao.addXp(userId, todayKey, bonus, now)
            profileDao.addXpAtomic(userId, bonus, now)
        }
    }

    internal suspend fun getCurrentStreak(userId: String): Int = withContext(Dispatchers.IO) {
        try {
            val activeDays = activityDao.getActiveDays(userId)
            StreakEngine.calculateStreak(activeDays, today(), yesterday(), ::previousDay)
        } catch (e: Exception) {
            crashlytics.logNonFatalException(e, "Streak calc error")
            0
        }
    }

    /** Placement band for new-word ordering (ADR-0010). Null when never taken. */
    suspend fun getPlacementCefr(userId: String): String? = withContext(Dispatchers.IO) {
        profileDao.getProfile(userId)?.placementCefr
    }

    suspend fun getDashboardData(userId: String): DashboardData = withContext(Dispatchers.IO) {
        val profile = getOrCreateProfile(userId)
        val todayKey = today()
        val now = System.currentTimeMillis()

        val todayXp = xpEventDao.getTodayXp(userId, todayKey)

        val totalXp = profile.totalXp
        val level = UserLevel.fromXp(totalXp)
        val levelProgress = UserLevel.levelProgress(totalXp)
        val xpToNextLevel = UserLevel.xpToNextLevel(totalXp)

        val todayActivity = activityDao.getActivityForDate(userId, todayKey)
        val totalActiveDays = activityDao.getTotalActiveDays(userId)

        val totalWordsViewed = activityDao.getTotalWordsViewed(userId)
        val totalSessionCount = activityDao.getTotalSessionCount(userId)
        val totalTimeMs = activityDao.getTotalTimeMs(userId)

        val totalWordsMastered = progressDao.getMasteredCount(userId)
        val totalWordsLearning = progressDao.getLearningCount(userId)
        val totalWordsNeedsReview = progressDao.getNeedsReviewCount(userId, now)
        val totalWordsLearned = totalWordsMastered + totalWordsLearning

        DashboardData(
            totalXp = totalXp,
            todayXp = todayXp,
            level = level,
            levelProgress = levelProgress,
            xpToNextLevel = xpToNextLevel,
            currentStreak = profile.currentStreak,
            bestStreak = profile.bestStreak,
            totalWordsViewed = totalWordsViewed,
            totalWordsLearned = totalWordsLearned,
            totalWordsMastered = totalWordsMastered,
            totalWordsLearning = totalWordsLearning,
            totalWordsNeedsReview = totalWordsNeedsReview,
            totalSessionCount = totalSessionCount,
            totalTimeMs = totalTimeMs,
            totalDaysActive = totalActiveDays,
            dailyGoalTarget = profile.dailyGoalXp,
            dailyGoalProgress = todayXp,
            dailyGoalMet = todayActivity?.dailyGoalMet ?: false,
            streakFreezes = profile.streakFreezes
        )
    }

    suspend fun getTodayActivity(userId: String): UserActivityEntity? =
        withContext(Dispatchers.IO) {
            activityDao.getActivityForDate(userId, today())
        }

    suspend fun getRecentActivity(userId: String, days: Int = 7): List<UserActivityEntity> =
        withContext(Dispatchers.IO) {
            activityDao.getRecentActivities(userId, days)
        }

    suspend fun updateDailyGoal(userId: String, goalXp: Int) =
        withContext(Dispatchers.IO) {
            profileDao.updateDailyGoal(userId, goalXp, System.currentTimeMillis())
        }

    suspend fun migrateLocalData(fromUserId: String, toUserId: String) =
        withContext(Dispatchers.IO) {
            try {
                // If target user already has data, just delete the anonymous records
                val existingProfile = profileDao.getProfile(toUserId)
                if (existingProfile != null) {
                    profileDao.deleteByUserId(fromUserId)
                    activityDao.deleteByUserId(fromUserId)
                } else {
                    activityDao.migrateUserId(fromUserId, toUserId)
                    profileDao.migrateUserId(fromUserId, toUserId)
                }
            } catch (e: Exception) {
                crashlytics.logNonFatalException(e, "Migration failed")
            }
        }
}
