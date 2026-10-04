package com.venom.harness

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.venom.analytics.StubAnalyticsManager
import com.venom.analytics.StubCrashlyticsManager
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
import com.venom.data.repo.UserActivityRepository
import com.venom.data.repo.UserWordProgressRepository
import com.venom.domain.model.CefrLevel
import com.venom.domain.model.WordMaster
import com.venom.domain.provider.AppConfigProvider
import com.venom.domain.repo.IRemoteConfig
import com.venom.domain.repo.IUserIdentityRepository
import com.venom.domain.repo.IWordMasterRepository
import com.venom.domain.srs.SrsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * Phase 0 VM test harness: the real repositories over functional in-memory DAO fakes
 * (same style as LearningLoopIntegrationTest), plus an identity/config/analytics fake
 * set, so WordMasteryViewModel and SpellingGameViewModel run on the JVM.
 */
class FakeIdentityRepository(private val userId: String = "u1") : IUserIdentityRepository {
    override val authState =
        MutableStateFlow(IUserIdentityRepository.AuthState(isSignedIn = true))
    override fun isSignedIn(): Boolean = true
    override suspend fun getCurrentUserId(): String = userId
}

class FakeRemoteConfig : IRemoteConfig {
    override suspend fun fetchAndActivate() {}
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = defaultValue
    override fun getLong(key: String, defaultValue: Long): Long = defaultValue
    override fun getDouble(key: String, defaultValue: Double): Double = defaultValue
    override fun getString(key: String, defaultValue: String): String = defaultValue
}

class FakeTransactions : TransactionRunner {
    override suspend fun <T> withTransaction(block: suspend () -> T): T = block()
}

class FakeProgressDao : UserWordProgressDao {
    val rows = java.util.concurrent.ConcurrentHashMap<Pair<String, Int>, UserWordProgressEntity>()
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

class FakeWordDao(
    words: List<WordMasterEntity>,
    private val progress: FakeProgressDao? = null,
) : WordMasterDao {
    private val store =
        java.util.concurrent.ConcurrentHashMap<Int, WordMasterEntity>()
            .apply { words.forEach { put(it.id, it) } }
    override fun getAllWords(): Flow<List<WordMasterEntity>> =
        flowOf(store.values.toList())
    override suspend fun getWordById(id: Int): WordMasterEntity? = store[id]
    override suspend fun getWordsByIds(ids: List<Int>): List<WordMasterEntity> =
        store.values.filter { it.id in ids }
    override suspend fun getWordsByLevel(level: String, limit: Int): List<WordMasterEntity> =
        store.values.filter { it.cefrLevel == level }.take(limit)
    override suspend fun getRandomWordsByCefr(level: String, limit: Int): List<WordMasterEntity> =
        store.values.filter { it.cefrLevel == level }.take(limit)
    override suspend fun getCountByCefr(level: String): Int =
        store.values.count { it.cefrLevel == level }
    override suspend fun getWordsByUnit(unitId: String): List<WordMasterEntity> = emptyList()
    override suspend fun getUnitsByLevel(level: String): List<String> = emptyList()
    override suspend fun getWordCount(): Int = store.size
    override suspend fun getRandomWords(limit: Int): List<WordMasterEntity> =
        store.values.take(limit)
    override suspend fun getNewWordsForUser(userId: String, limit: Int): List<WordMasterEntity> =
        store.values
            .filter { progress?.rows?.get(userId to it.id) == null }
            .sortedBy { it.rank }
            .take(limit)
    override suspend fun countNewWordsForUser(userId: String): Int =
        store.values.count { w -> progress?.rows?.get(userId to w.id) == null }
    override suspend fun getRandomUnseenWordsForUser(
        userId: String,
        limit: Int
    ): List<WordMasterEntity> = store.values.take(limit)
    override suspend fun getRandomNotMasteredWordsForUser(
        userId: String,
        excludeIds: List<Int>,
        limit: Int
    ): List<WordMasterEntity> = store.values.filter { it.id !in excludeIds }.take(limit)
    override suspend fun insertAll(words: List<WordMasterEntity>) {
        words.forEach { insert(it) }
    }
    override suspend fun insert(word: WordMasterEntity) {
        store[word.id] = word
    }
}

class FakeQuizAttemptDao : QuizAttemptDao {
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

class FakeActivityDao : UserActivityDao {
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
    override suspend fun getTotalXp(userId: String): Int? =
        rows.values.filter { it.userId == userId }.sumOf { it.totalXpEarned }
    override suspend fun getTotalWordsViewed(userId: String): Int =
        rows.values.filter { it.userId == userId }.sumOf { it.wordsViewed }
    override suspend fun getTotalSessionCount(userId: String): Int =
        rows.values.filter { it.userId == userId }.sumOf { it.sessionCount }
    override suspend fun getTotalTimeMs(userId: String): Long =
        rows.values.filter { it.userId == userId }.sumOf { it.totalTimeMs }
    override suspend fun incrementWordsViewed(userId: String, dateKey: String, now: Long) {
        bump(userId, dateKey) { it.copy(wordsViewed = it.wordsViewed + 1) }
    }
    override suspend fun incrementRecallSuccess(
        userId: String,
        dateKey: String,
        xp: Int,
        now: Long
    ) {
        bump(userId, dateKey) {
            it.copy(recallSuccess = it.recallSuccess + 1, totalXpEarned = it.totalXpEarned + xp)
        }
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
        bump(userId, dateKey) {
            it.copy(
                productionSuccess = it.productionSuccess + 1,
                wordsPracticed = it.wordsPracticed + 1,
                totalXpEarned = it.totalXpEarned + xp
            )
        }
    }
    override suspend fun incrementMastered(userId: String, dateKey: String, xp: Int, now: Long) {
        bump(userId, dateKey) {
            it.copy(masteredCount = it.masteredCount + 1, totalXpEarned = it.totalXpEarned + xp)
        }
    }
    override suspend fun addXp(userId: String, dateKey: String, xp: Int, now: Long) {
        bump(userId, dateKey) { it.copy(totalXpEarned = it.totalXpEarned + xp) }
    }
    override suspend fun addSessionTime(
        userId: String,
        dateKey: String,
        durationMs: Long,
        now: Long
    ) {
        bump(userId, dateKey) { it.copy(totalTimeMs = it.totalTimeMs + durationMs) }
    }
    override suspend fun incrementSessionCount(userId: String, dateKey: String, now: Long) {
        bump(userId, dateKey) { it.copy(sessionCount = it.sessionCount + 1) }
    }
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
    private fun bump(
        userId: String,
        dateKey: String,
        f: (UserActivityEntity) -> UserActivityEntity
    ) {
        val current = rows[userId to dateKey] ?: return
        rows[userId to dateKey] = f(current)
    }
}

class FakeProfileDao : UserProfileDao {
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

class FakeXpEventDao : XpEventDao {
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

class FakeWordMasterRepository(private val wordDao: FakeWordDao) : IWordMasterRepository {
    override suspend fun getWordById(id: Int): WordMaster? =
        wordDao.getWordById(id)?.let { com.venom.data.mapper.WordMasterEntityMapper.toDomain(it) }
    override suspend fun getWordsByIds(ids: List<Int>): List<WordMaster> =
        wordDao.getWordsByIds(ids).map { com.venom.data.mapper.WordMasterEntityMapper.toDomain(it) }
    override suspend fun getWordsByLevel(level: CefrLevel, limit: Int): List<WordMaster> =
        wordDao.getWordsByLevel(level.name, limit)
            .map { com.venom.data.mapper.WordMasterEntityMapper.toDomain(it) }
    override suspend fun getWordsByCefr(cefrLevel: CefrLevel, limit: Int): List<WordMaster> =
        wordDao.getRandomWordsByCefr(cefrLevel.name, limit)
            .map { com.venom.data.mapper.WordMasterEntityMapper.toDomain(it) }
    override suspend fun getLevelProgress(level: com.venom.domain.model.WordLevels): Float = 0f
    override suspend fun getLevelsProgress(): Map<String, Float> = emptyMap()
    override suspend fun getWordCount(): Int = wordDao.getWordCount()
    override suspend fun insertWords(words: List<WordMaster>) {}
    override suspend fun getRandomWords(limit: Int): List<WordMaster> =
        wordDao.getRandomWords(limit)
            .map { com.venom.data.mapper.WordMasterEntityMapper.toDomain(it) }
}

class VmSessionHarness(
    val userId: String = "u1",
    words: List<WordMasterEntity> = listOf(testWordEntity(7))
) {
    val progressDao = FakeProgressDao()
    val wordDao = FakeWordDao(words, progressDao)
    val quizDao = FakeQuizAttemptDao()
    val activityDao = FakeActivityDao()
    val profileDao = FakeProfileDao()
    val xpDao = FakeXpEventDao()
    val identity = FakeIdentityRepository(userId)

    val activityRepository = UserActivityRepository(
        activityDao = activityDao,
        profileDao = profileDao,
        xpEventDao = xpDao,
        progressDao = progressDao,
        crashlytics = StubCrashlyticsManager(),
        appConfigProvider = AppConfigProvider(FakeRemoteConfig())
    )
    val progressRepository = UserWordProgressRepository(
        progressDao = progressDao,
        wordMasterDao = wordDao,
        quizAttemptDao = quizDao,
        srsEngine = SrsEngine(),
        activityRepository = activityRepository,
        crashlytics = StubCrashlyticsManager(),
        transactions = FakeTransactions()
    )

    private val dataStoreScope = CoroutineScope(
        SupervisorJob() + kotlinx.coroutines.Dispatchers.IO.limitedParallelism(1)
    )
    val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = dataStoreScope) {
        File.createTempFile("vm_harness", ".preferences_pb").apply { deleteOnExit() }
    }

    fun close() {
        dataStoreScope.cancel()
    }

    /**
     * Real-time bounded wait for async repository work (Dispatchers.IO inside the
     * repositories) while yielding to the Main test scheduler between polls.
     */
    suspend fun await(
        scheduler: kotlinx.coroutines.test.TestCoroutineScheduler,
        timeoutMs: Long = 5_000,
        cond: suspend () -> Boolean
    ) {
        val start = System.currentTimeMillis()
        while (!cond()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                throw AssertionError("condition not met within ${timeoutMs}ms")
            }
            scheduler.advanceUntilIdle()
            kotlinx.coroutines.delay(10)
        }
        scheduler.advanceUntilIdle()
    }
}

fun testWordEntity(id: Int): WordMasterEntity = WordMasterEntity(
    id = id,
    wordEn = "obtain",
    pos = "verb",
    cefrLevel = "B1",
    rank = 1000,
    frequency = 3,
    difficultyScore = 5,
    phoneticUs = "əbˈteɪn",
    phoneticAr = "أوبتين",
    definitionEn = "To get or acquire something.",
    arabicAr = "يحصل على"
)

fun testWord(id: Int = 7): WordMaster = WordMaster(
    id = id,
    wordEn = "obtain",
    pos = "verb",
    cefrLevel = CefrLevel.B1,
    fromOxford = 1,
    rank = 1000,
    frequency = 3,
    difficultyScore = 5,
    phoneticUs = "əbˈteɪn",
    phoneticUk = null,
    phoneticAr = "أوبتين",
    translit = "obtain",
    syllabify = "ob•tain",
    definitionEn = "To get or acquire something.",
    definitionAr = "يحصل على شيء ما",
    usageNote = null,
    category = null,
    primarySense = null,
    semanticTags = emptyList(),
    register = "Neutral",
    mnemonicAr = null,
    wordFamily = com.venom.domain.model.WordFamily(noun = "obtainment"),
    synonyms = listOf("acquire"),
    antonyms = listOf("lose"),
    examples = mapOf(CefrLevel.B1 to "She obtained her degree last year."),
    collocations = listOf("obtain permission"),
    relatedWords = com.venom.domain.model.RelatedWords(emptyList(), emptyList()),
    arabicAr = "يحصل على"
)
