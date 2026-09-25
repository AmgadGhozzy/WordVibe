package com.venom.data.repo

import android.util.Log
import com.venom.analytics.CrashlyticsManager
import com.venom.data.local.TransactionRunner
import com.venom.data.local.dao.QuizAttemptDao
import com.venom.data.local.dao.UserWordProgressDao
import com.venom.data.local.dao.WordMasterDao
import com.venom.data.local.entity.QuizAttemptEntity
import com.venom.data.local.entity.UserWordProgressEntity
import com.venom.data.local.entity.WordMasterEntity
import com.venom.data.mapper.WordMasterEntityMapper
import com.venom.domain.model.KnownState
import com.venom.domain.model.UserWordProgress
import com.venom.domain.srs.SrsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserWordProgressRepository @Inject constructor(
    private val progressDao: UserWordProgressDao,
    private val wordMasterDao: WordMasterDao,
    private val quizAttemptDao: QuizAttemptDao,
    private val srsEngine: SrsEngine,
    private val activityRepository: UserActivityRepository,
    private val crashlytics: CrashlyticsManager,
    private val transactions: TransactionRunner
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun today(): String = dateFormat.format(Date())

    suspend fun getOrCreateProgress(userId: String, wordId: Int): UserWordProgress =
        withContext(Dispatchers.IO) {
            progressDao.getProgress(userId, wordId)?.toDomain()
                ?: UserWordProgress(userId = userId, wordId = wordId).also {
                    progressDao.upsert(it.toEntity())
                    activityRepository.recordNewWord(userId)
                }
        }

    suspend fun getWordsDueForReview(
        userId: String,
        limit: Int = 10
    ): List<com.venom.domain.model.WordMaster> =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val dueProgress = progressDao.getDueWords(userId, now, limit)
            dueProgress.mapNotNull { progress ->
                wordMasterDao.getWordById(progress.wordId)?.let {
                    WordMasterEntityMapper.toDomain(it)
                }
            }
        }

    fun getBookmarkedWords(userId: String): Flow<List<UserWordProgress>> =
        progressDao.getBookmarkedWords(userId).map { list -> list.map { it.toDomain() } }

    /**
     * Shared guard for recall writes: unknown words are rejected loudly, and an already
     * APPLIED idempotency key is a no-op. Returns the word plus whether this call must
     * persist a fresh event row (false when replaying a crashed, unapplied event).
     */
    private data class ResolvedAttempt(
        val word: WordMasterEntity,
        val persistEvent: Boolean
    )

    private suspend fun resolveWordForAttempt(
        wordId: Int,
        signal: String,
        idempotencyKey: String?
    ): ResolvedAttempt? {
        val word = wordMasterDao.getWordById(wordId)
        if (word == null) {
            crashlytics.logNonFatalException(
                IllegalStateException("$signal for unknown word $wordId"),
                "Rejected recall event: word missing from store"
            )
            return null
        }
        val existing = idempotencyKey?.let { quizAttemptDao.getByIdempotencyKey(it) }
        if (existing != null && existing.projectionApplied) return null
        return ResolvedAttempt(word, persistEvent = existing == null)
    }

    /** Applies the SRS projection + activity for one recall outcome (no event row here). */
    private suspend fun applyRecallOutcome(
        userId: String,
        word: WordMasterEntity,
        wordId: Int,
        isCorrect: Boolean
    ) {
        val before = getOrCreateProgress(userId, wordId)
        val updated = if (isCorrect) {
            srsEngine.onRecallSuccess(before, word.rank ?: 0, word.frequency ?: 1)
        } else {
            srsEngine.onRecallFail(before)
        }
        progressDao.upsert(updated.toEntity())

        if (isCorrect) {
            if (updated.knownState == KnownState.MASTERED &&
                before.knownState != KnownState.MASTERED
            ) {
                activityRepository.recordWordMastered(userId, wordId)
            }
            activityRepository.recordRecallSuccess(userId, wordId)
        } else {
            activityRepository.recordRecallFail(userId)
        }
    }

    /**
     * Reconciles crashed sessions: every durable event whose projection never applied gets
     * projected exactly once. Unknown-word rows are drained (marked applied) with a loud
     * log since no projection is possible for them. Finalization is never the rescue path.
     */
    suspend fun replayUnappliedEvents(userId: String, limit: Int = 100) =
        withContext(Dispatchers.IO) {
            quizAttemptDao.getUnapplied(userId, limit).forEach { attempt ->
                val key = attempt.idempotencyKey ?: return@forEach
                transactions.withTransaction {
                    val word = wordMasterDao.getWordById(attempt.wordId)
                    if (word == null) {
                        crashlytics.logNonFatalException(
                            IllegalStateException(
                                "replay for unknown word ${attempt.wordId}"
                            ),
                            "Draining unprojectable event"
                        )
                        quizAttemptDao.markApplied(key)
                        return@withTransaction
                    }
                    applyRecallOutcome(userId, word, attempt.wordId, attempt.isCorrect)
                    quizAttemptDao.markApplied(key)
                }
            }
        }

    suspend fun recordRecallSuccess(
        userId: String,
        wordId: Int,
        idempotencyKey: String? = null,
        sessionId: String? = null
    ) = recordRecall(userId, wordId, idempotencyKey, sessionId, isCorrect = true)

    suspend fun recordRecallFail(
        userId: String,
        wordId: Int,
        idempotencyKey: String? = null,
        sessionId: String? = null
    ) = recordRecall(userId, wordId, idempotencyKey, sessionId, isCorrect = false)

    private suspend fun recordRecall(
        userId: String,
        wordId: Int,
        idempotencyKey: String?,
        sessionId: String?,
        isCorrect: Boolean
    ) = withContext(Dispatchers.IO) {
        transactions.withTransaction {
            val resolved = resolveWordForAttempt(
                wordId,
                if (isCorrect) "recall success" else "recall failure",
                idempotencyKey
            ) ?: return@withTransaction

            if (resolved.persistEvent) {
                quizAttemptDao.insert(
                    QuizAttemptEntity(
                        userId = userId,
                        wordId = wordId,
                        dateKey = today(),
                        questionType = "RECALL",
                        source = "FLASHCARD",
                        isCorrect = isCorrect,
                        selectedAnswer = "",
                        correctAnswer = resolved.word.wordEn ?: "",
                        idempotencyKey = idempotencyKey,
                        sessionId = sessionId,
                        projectionApplied = false
                    )
                )
            }

            applyRecallOutcome(userId, resolved.word, wordId, isCorrect)
            if (idempotencyKey != null) quizAttemptDao.markApplied(idempotencyKey)
        }
    }

    suspend fun recordProductionSuccess(userId: String, wordId: Int) =
        withContext(Dispatchers.IO) {
            transactions.withTransaction {
                val word = wordMasterDao.getWordById(wordId) ?: return@withTransaction

                quizAttemptDao.insert(
                    QuizAttemptEntity(
                        userId = userId,
                        wordId = wordId,
                        dateKey = today(),
                        questionType = "PRODUCTION",
                        source = "FLASHCARD",
                        isCorrect = true,
                        selectedAnswer = "",
                        correctAnswer = word.wordEn ?: ""
                    )
                )

                val before = getOrCreateProgress(userId, wordId)
                val updated =
                    srsEngine.onProductionSuccess(before, word.rank ?: 0, word.frequency ?: 1)
                progressDao.upsert(updated.toEntity())

                if (updated.knownState == KnownState.MASTERED &&
                    before.knownState != KnownState.MASTERED
                ) {
                    activityRepository.recordWordMastered(userId, wordId)
                }

                activityRepository.recordPracticeSuccess(userId, wordId)
            }
        }

    suspend fun recordView(userId: String, wordId: Int) = withContext(Dispatchers.IO) {
        ensureProgressExists(userId, wordId)
        activityRepository.recordWordView(userId, wordId)
    }

    suspend fun toggleBookmark(userId: String, wordId: Int, bookmarked: Boolean) =
        withContext(Dispatchers.IO) {
            ensureProgressExists(userId, wordId)
            progressDao.setBookmarked(userId, wordId, bookmarked)
        }

    suspend fun migrateLocalData(fromUserId: String, toUserId: String) =
        withContext(Dispatchers.IO) {
            try {
                val fromCount = progressDao.getProgressCount(fromUserId)
                if (fromCount == 0) return@withContext

                val toCount = progressDao.getProgressCount(toUserId)
                if (toCount > 0) {
                    // Target user already has progress — delete stale anonymous data
                    progressDao.deleteByUserId(fromUserId)
                    Log.d(TAG, "Deleted $fromCount anonymous progress records (target has $toCount)")
                } else {
                    progressDao.migrateUserId(fromUserId, toUserId)
                    Log.d(TAG, "Migrated $fromCount word progress records: $fromUserId → $toUserId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Migration failed: ${e.message}", e)
                crashlytics.logNonFatalException(e, "Word progress migration failed")
            }
        }

    private suspend fun ensureProgressExists(userId: String, wordId: Int) {
        if (progressDao.getProgress(userId, wordId) == null) {
            progressDao.upsert(UserWordProgress(userId = userId, wordId = wordId).toEntity())
        }
    }

    private fun UserWordProgressEntity.toDomain() = UserWordProgress(
        userId = userId,
        wordId = wordId,
        firstSeen = firstSeen,
        bookmarked = bookmarked,
        productionSuccess = productionSuccess,
        recallSuccess = recallSuccess,
        totalAttempts = totalAttempts,
        knownState = try { KnownState.valueOf(knownState) } catch (_: Exception) { KnownState.NEW },
        difficulty = difficulty,
        stability = stability,
        lapsesCount = lapsesCount,
        lastReview = lastReview,
        nextReview = nextReview
    )

    private fun UserWordProgress.toEntity() = UserWordProgressEntity(
        userId = userId,
        wordId = wordId,
        firstSeen = firstSeen,
        bookmarked = bookmarked,
        productionSuccess = productionSuccess,
        recallSuccess = recallSuccess,
        totalAttempts = totalAttempts,
        knownState = knownState.name,
        difficulty = difficulty,
        stability = stability,
        lapsesCount = lapsesCount,
        lastReview = lastReview,
        nextReview = nextReview
    )

    companion object {
        private const val TAG = "UserWordProgressRepository"
    }
}
