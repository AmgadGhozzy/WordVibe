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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * PERMANENT CONTAINER RULE (ticket 11, PM round-7): the shared fakes below are
 * written from repository consumers running on Dispatchers.IO and read back from
 * test threads, so every mutable container in a fixture MUST be thread-safe:
 * ConcurrentHashMap for maps and CopyOnWriteArrayList for lists. The unsanctioned
 * mutable constructors (mutableMapOf / mutableListOf / raw ArrayList) are banned
 * in this whole file, comment lines aside — HarnessContainerHygieneTest enforces
 * the ban mechanically. Extend the fakes with those two thread-safe types only.
 */

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
    private val mutex = Mutex()

    /**
     * Serialized (PM round-13 item 3): production Room runs withTransaction blocks
     * atomically, so the fake must not let concurrent transactional blocks interleave
     * either — otherwise the same read-modify-write lost-update family fixed in
     * ticket 11 could reproduce in the test lens only. Fair (FIFO) Mutex = blocks
     * execute one at a time in acquisition order. Non-reentrant is safe: no
     * withTransaction site ever nests another one.
     */
    override suspend fun <T> withTransaction(block: suspend () -> T): T =
        mutex.withLock { block() }
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
        override suspend fun getKnownCount(userId: String): Int = 0
    override suspend fun getLearningCount(userId: String): Int =
        rows.values.count {
            it.userId == userId && (it.knownState == "LEARNING" || it.knownState == "KNOWN")
        }
    override suspend fun getNeedsReviewCount(userId: String, now: Long): Int =
        rows.values.count {
            it.userId == userId && it.nextReview != null && it.nextReview <= now
        }
    override suspend fun getScheduledReviewTimes(userId: String, fromTime: Long): List<Long> =
        rows.values.filter {
            it.userId == userId && it.nextReview != null && it.nextReview >= fromTime
        }.map { it.nextReview!! }
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
        limit: Int
    ): List<WordMasterEntity> = store.values.filter { w ->
        progress?.rows?.get(userId to w.id)?.knownState != "MASTERED"
    }.take(limit)
    override suspend fun insertAll(words: List<WordMasterEntity>) {
        words.forEach { insert(it) }
    }
    override suspend fun insertAllOrIgnore(words: List<WordMasterEntity>) {
        words.forEach { if (store[it.id] == null) insert(it) }
    }
    override suspend fun insert(word: WordMasterEntity) {
        store[word.id] = word
    }
}

class FakeQuizAttemptDao : QuizAttemptDao {
    val events = java.util.concurrent.CopyOnWriteArrayList<QuizAttemptEntity>()
    // Diagnostic counters (round-8 item 2): call counts for the durable-tap persist path.
    val insertCalls = java.util.concurrent.atomic.AtomicInteger()
    val getByIdCalls = java.util.concurrent.atomic.AtomicInteger()
    val markAppliedCalls = java.util.concurrent.atomic.AtomicInteger()
    override suspend fun insert(attempt: QuizAttemptEntity) {
        insertCalls.incrementAndGet()
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
    override suspend fun getByIdempotencyKey(key: String): QuizAttemptEntity? {
        getByIdCalls.incrementAndGet()
        return events.firstOrNull { it.idempotencyKey == key }
    }
    override suspend fun getProductionAttemptCounts(
        userId: String
    ): List<com.venom.data.local.dao.ProductionAttemptCount> =
        events.filter { it.userId == userId && it.questionType == "PRODUCTION" }
            .groupingBy { it.wordId }
            .eachCount()
            .map { (wordId, attempts) -> com.venom.data.local.dao.ProductionAttemptCount(wordId, attempts) }
    override suspend fun getUnapplied(userId: String, limit: Int): List<QuizAttemptEntity> =
        events.filter {
            !it.projectionApplied && it.idempotencyKey != null && it.userId == userId
        }.take(limit)
    override suspend fun markApplied(key: String) {
        markAppliedCalls.incrementAndGet()
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
    val rows = java.util.concurrent.ConcurrentHashMap<Pair<String, String>, UserActivityEntity>()

    // Diagnostic counters + exception ledger (PM round-10, ticket-11 forensics):
    // the activity path behind ensureTodayExists and the finalize waits.
    // ensureTodayExists is repository-level; its DAO footprint is
    // getActivityForDate + upsert, which is what these counters observe.
    // bumpNoRow: bump() silently no-ops when the day row is missing — the exact
    // silent shape that would keep a getTotalSessionCount condition from ever
    // materializing.
    val upsertCalls = java.util.concurrent.atomic.AtomicInteger()
    val getActivityForDateCalls = java.util.concurrent.atomic.AtomicInteger()
    val incrementSessionCountCalls = java.util.concurrent.atomic.AtomicInteger()
    val getTotalSessionCountCalls = java.util.concurrent.atomic.AtomicInteger()
    val bumpNoRowCalls = java.util.concurrent.atomic.AtomicInteger()
    val diagExceptionCount = java.util.concurrent.atomic.AtomicInteger()
    val diagExceptions = java.util.concurrent.CopyOnWriteArrayList<String>()

    // PM round-11 forensics: (1) lastTotalSessionValue — the last value
    // getTotalSessionCount actually RETURNED (increment included or not), so an
    // await-exhausted dump answers directly whether the awaited read kept seeing
    // 0, 2, or something else. (2) rowWriteLog — the row-write sequence: one
    // entry per PUT on rows (bump writes and upserts), recording an increasing
    // seq, the caller identity passed down to bump, the written sessionCount,
    // the writer thread, and a field fingerprint (totalTimeMs / recallSuccess /
    // totalXpEarned) that distinguishes incrementSessionCount vs addSessionTime
    // vs incrementRecallSuccess vs addXp. This is the lost-update witness: if
    // two coroutines interleave bump's read→write gap, the log shows a later
    // seq writing back a STALE sessionCount over an earlier larger one.
    val lastTotalSessionValue = java.util.concurrent.atomic.AtomicReference<Int?>(null)
    val rowWriteSeq = java.util.concurrent.atomic.AtomicInteger()
    val rowWriteLog = java.util.concurrent.CopyOnWriteArrayList<String>()

    private fun recordRowWrite(source: String, key: Pair<String, String>, written: UserActivityEntity) {
        val seq = rowWriteSeq.incrementAndGet()
        rowWriteLog.add(
            "#$seq $source key=${key.first}|${key.second} " +
                "thread=${Thread.currentThread().name} " +
                "sc=${written.sessionCount} tm=${written.totalTimeMs} " +
                "rs=${written.recallSuccess} xp=${written.totalXpEarned}"
        )
        // Keep only the LAST 64 entries (bounded forensic window); per-op atomic
        // on CopyOnWriteArrayList, so no corruption under concurrent writers.
        while (rowWriteLog.size > 64) rowWriteLog.removeAt(0)
    }

    private fun recordDiagException(method: String, t: Throwable) {
        diagExceptionCount.incrementAndGet()
        if (diagExceptions.size < 16) {
            diagExceptions.add("$method: ${t::class.simpleName}: ${t.message}")
        }
    }

    override suspend fun upsert(entity: UserActivityEntity) {
        upsertCalls.incrementAndGet()
        try {
            val key = entity.userId to entity.dateKey
            rows[key] = entity
            recordRowWrite("upsert", key, entity)
        } catch (t: Throwable) {
            recordDiagException("upsert", t)
            throw t
        }
    }

    override suspend fun insertOrIgnore(entity: UserActivityEntity): Long {
        upsertCalls.incrementAndGet()
        return try {
            val key = entity.userId to entity.dateKey
            // Atomic putIfAbsent: mirrors Room's INSERT OR IGNORE. A naive
            // containsKey+put here re-introduced the ticket-11 lost-update
            // race between concurrent day-start callers.
            val previous = rows.putIfAbsent(key, entity)
            if (previous == null) {
                recordRowWrite("insertOrIgnore", key, entity)
                1L
            } else {
                -1L
            }
        } catch (t: Throwable) {
            recordDiagException("insertOrIgnore", t)
            throw t
        }
    }
    override suspend fun getActivityForDate(
        userId: String,
        dateKey: String
    ): UserActivityEntity? {
        getActivityForDateCalls.incrementAndGet()
        return try {
            rows[userId to dateKey]
        } catch (t: Throwable) {
            recordDiagException("getActivityForDate", t)
            throw t
        }
    }
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
    override suspend fun getTotalSessionCount(userId: String): Int {
        getTotalSessionCountCalls.incrementAndGet()
        return try {
            rows.values.filter { it.userId == userId }.sumOf { it.sessionCount }
                .also { lastTotalSessionValue.set(it) }
        } catch (t: Throwable) {
            recordDiagException("getTotalSessionCount", t)
            throw t
        }
    }
    override suspend fun getTotalTimeMs(userId: String): Long =
        rows.values.filter { it.userId == userId }.sumOf { it.totalTimeMs }
    override suspend fun incrementWordsViewed(userId: String, dateKey: String, now: Long) {
        bump(userId, dateKey, "incrementWordsViewed") { it.copy(wordsViewed = it.wordsViewed + 1) }
    }
    override suspend fun incrementRecallSuccess(
        userId: String,
        dateKey: String,
        xp: Int,
        now: Long
    ) {
        bump(userId, dateKey, "incrementRecallSuccess") {
            it.copy(recallSuccess = it.recallSuccess + 1, totalXpEarned = it.totalXpEarned + xp)
        }
    }
    override suspend fun incrementRecallFail(userId: String, dateKey: String, now: Long) {
        bump(userId, dateKey, "incrementRecallFail") { it.copy(recallFail = it.recallFail + 1) }
    }
    override suspend fun incrementPracticeSuccess(
        userId: String,
        dateKey: String,
        xp: Int,
        now: Long
    ) {
        bump(userId, dateKey, "incrementPracticeSuccess") {
            it.copy(
                productionSuccess = it.productionSuccess + 1,
                wordsPracticed = it.wordsPracticed + 1,
                totalXpEarned = it.totalXpEarned + xp
            )
        }
    }
    override suspend fun incrementMastered(userId: String, dateKey: String, xp: Int, now: Long) {
        bump(userId, dateKey, "incrementMastered") {
            it.copy(masteredCount = it.masteredCount + 1, totalXpEarned = it.totalXpEarned + xp)
        }
    }
    override suspend fun addXp(userId: String, dateKey: String, xp: Int, now: Long) {
        bump(userId, dateKey, "addXp") { it.copy(totalXpEarned = it.totalXpEarned + xp) }
    }
    override suspend fun addSessionTime(
        userId: String,
        dateKey: String,
        durationMs: Long,
        now: Long
    ) {
        bump(userId, dateKey, "addSessionTime") { it.copy(totalTimeMs = it.totalTimeMs + durationMs) }
    }
    override suspend fun incrementSessionCount(userId: String, dateKey: String, now: Long) {
        incrementSessionCountCalls.incrementAndGet()
        try {
            bump(userId, dateKey, "incrementSessionCount") { it.copy(sessionCount = it.sessionCount + 1) }
        } catch (t: Throwable) {
            recordDiagException("incrementSessionCount", t)
            throw t
        }
    }
    override suspend fun markDailyGoalMet(
        userId: String,
        dateKey: String,
        bonusXp: Int,
        now: Long
    ) {
        bump(userId, dateKey, "markDailyGoalMet") {
            it.copy(dailyGoalMet = true, totalXpEarned = it.totalXpEarned + bonusXp)
        }
    }
    override suspend fun migrateUserId(fromUserId: String, toUserId: String) {}
    override suspend fun deleteByUserId(userId: String) {}
    // ATOMIC since ticket-11 confirmation (PM standing approval): the old shape
    // read `rows[key]` and then wrote `rows[key] = f(current)` — a non-atomic
    // read-modify-write whose interleaved gap was CAPTURED red-handed
    // (.scratch/ticket11-diag/r11-losupdate-confirmed/: row-write #11
    // incrementRecallSuccess wrote back a stale full-row copy over #10's
    // sessionCount=1, so getTotalSessionCount never saw the session land).
    // compute fuses read→transform→write into one step under the CHM bin lock.
    // Row-absent stays a no-op (no insert), counted by bumpNoRowCalls inside
    // the mapping function; all counters, rowWriteLog and diagDump shapes are
    // unchanged. `source` is only forensic attribution for the rowWriteLog,
    // recorded after the put exactly as before.
    private fun bump(
        userId: String,
        dateKey: String,
        source: String,
        f: (UserActivityEntity) -> UserActivityEntity
    ) {
        val key = userId to dateKey
        var written: UserActivityEntity? = null
        rows.compute(key) { _, current ->
            if (current == null) {
                bumpNoRowCalls.incrementAndGet()
                return@compute null
            }
            f(current).also { written = it }
        }
        written?.let { recordRowWrite(source, key, it) }
    }
}

class FakeProfileDao : UserProfileDao {
    val store = java.util.concurrent.ConcurrentHashMap<String, UserProfileEntity>()
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
        override suspend fun grantStreakFreeze(userId: String, now: Long) {
            val current = store[userId] ?: return
            if (current.streakFreezes < 2) {
                store[userId] = current.copy(streakFreezes = current.streakFreezes + 1)
            }
        }
    override suspend fun consumeStreakFreeze(userId: String, now: Long) {
        val current = store[userId] ?: return
        if (current.streakFreezes > 0) {
            store[userId] = current.copy(streakFreezes = current.streakFreezes - 1)
        }
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
    ) {
        val current = store[userId] ?: UserProfileEntity(userId = userId)
        store[userId] = current.copy(
            placementUnit = unit,
            placementCefr = cefr,
            placementTakenAt = takenAt,
            totalXp = assignedXp,
            updatedAt = now
        )
    }
    override suspend fun getPlacementUnit(userId: String): Int? = null
    override suspend fun deleteByUserId(userId: String) {}
}

class FakeXpEventDao : XpEventDao {
    val events = java.util.concurrent.CopyOnWriteArrayList<XpEventEntity>()
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
    override suspend fun countWordsByCefr(level: CefrLevel): Int = wordDao.getCountByCefr(level.name)
    override suspend fun insertWords(words: List<WordMaster>) {}
    override suspend fun insertWordsOrIgnore(words: List<WordMaster>) {}
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

    // R2 root fix: the producer hands back a unique path that does NOT pre-exist —
    // DataStore creates the file itself, so no write ever renames onto a
    // pre-created empty target.
    private val dataStoreFile: File = File.createTempFile("vm_harness", ".preferences_pb")
        .apply { deleteOnExit(); delete() }

    private val dataStoreFailures = java.util.concurrent.CopyOnWriteArrayList<Throwable>()

    // A late actor failure is captured here and rethrown by close(), so it fails the
    // OWNING test instead of leaking as a process-level uncaught exception that
    // attributes to whichever suite runs next.
    private val dataStoreGuard = CoroutineExceptionHandler { _, throwable ->
        dataStoreFailures += throwable
    }

    private val dataStoreScope = CoroutineScope(
        SupervisorJob() + kotlinx.coroutines.test.UnconfinedTestDispatcher() + dataStoreGuard
    )
    val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = dataStoreScope) {
        dataStoreFile
    }

    private val diagSeq = diagSequence.incrementAndGet()

    /**
     * Writes one per-harness snapshot file (call counters) under
     * build/harness-counters/. Called at close() — @AfterEach always runs, even
     * after a failed test — and at await-exhaustion, so a failing test's
     * counters survive for forensics (ticket 11 / round-8 item 2).
     */
    fun diagDump(tag: String) {
        runCatching {
            val dir = java.io.File("build/harness-counters")
            dir.mkdirs()
            val stamp = System.currentTimeMillis().toString() + "-" +
                (System.nanoTime() % 1_000_000_000L).toString()
            val body = buildString {
                append(
                    "WV-DIAG seq=$diagSeq ts=${System.nanoTime()} " +
                        "thread=${Thread.currentThread().name} tag=$tag " +
                        "insertCalls=${quizDao.insertCalls.get()} " +
                        "getByIdCalls=${quizDao.getByIdCalls.get()} " +
                        "markAppliedCalls=${quizDao.markAppliedCalls.get()} " +
                        "actUpsert=${activityDao.upsertCalls.get()} " +
                        "actGetForDate=${activityDao.getActivityForDateCalls.get()} " +
                        "actIncSession=${activityDao.incrementSessionCountCalls.get()} " +
                        "actGetTotalSession=${activityDao.getTotalSessionCountCalls.get()} " +
                        "actBumpNoRow=${activityDao.bumpNoRowCalls.get()} " +
                        "actExceptions=${activityDao.diagExceptionCount.get()} " +
                        "actLastTotal=${activityDao.lastTotalSessionValue.get()}" +
                        (if (activityDao.diagExceptions.isEmpty()) ""
                         else " exc=${activityDao.diagExceptions.joinToString("; ")}") + "\n"
                )
                val writes = activityDao.rowWriteLog
                if (writes.isNotEmpty()) {
                    append(
                        "ROW-WRITES total=${activityDao.rowWriteSeq.get()} " +
                            "captured=${writes.size}\n"
                    )
                    writes.forEach { append("  $it\n") }
                }
            }
            java.io.File(dir, "$stamp.txt").writeText(body)
        }
    }

    fun close() {
        diagDump("close")
        // Drain first: give the actor a quiescent point so an in-flight write is never
        // cut mid-rename by the cancellation below.
        runCatching {
            runBlocking { withTimeoutOrNull(2_000) { dataStore.data.first() } }
        }
        dataStoreScope.cancel()
        dataStoreFailures.firstOrNull()?.let { throw it }
    }

    /**
     * Deterministic wait: drains the Main test scheduler first — advanceUntilIdle
     * runs every queued task to completion immediately — then, because the real
     * repositories hop through Dispatchers.IO (which no test API virtualizes),
     * yields briefly so in-flight IO can land and re-drains. There is NO
     * wall-clock budget: the give-up rule is count-based — after MAX_IDLE_POLLS
     * consecutive scheduler-drained checks with the condition still unmet, the
     * wait fails. A loaded machine gets as many polls as it needs (each is
     * cheap); only a condition that never materializes fails.
     */
    suspend fun await(
        scheduler: kotlinx.coroutines.test.TestCoroutineScheduler,
        cond: suspend () -> Boolean
    ) {
        scheduler.advanceUntilIdle()
        var idlePolls = 0
        while (!cond()) {
            kotlinx.coroutines.delay(10)
            scheduler.advanceUntilIdle()
            if (cond()) break
            idlePolls++
            if (idlePolls >= MAX_IDLE_POLLS) {
                diagDump("AWAIT-EXHAUSTED: condition not met after $MAX_IDLE_POLLS idle polls")
                throw AssertionError(
                    "condition not met: scheduler drained on all $MAX_IDLE_POLLS consecutive idle polls"
                )
            }
        }
        scheduler.advanceUntilIdle()
    }
}

private const val MAX_IDLE_POLLS = 3_000

private val diagSequence = java.util.concurrent.atomic.AtomicLong()

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
    arabicAr = "يحصل على",
    productionExpected = "obtain",
    productionVariants = listOf("acquire")
)
