package com.venom.stackcard.session

import com.venom.domain.model.WordMaster

/**
 * Pure session decision engine for the flashcard flow (Step 3, zero behavior
 * change). It owns ONLY the decisions that used to live inline in
 * WordMasteryViewModel: tap keys (session:word:outcome:ordinal), the visible/
 * removed card lists, the current word, processed/needs-review counters, and
 * the finished flag. It performs no I/O and knows nothing about databases,
 * coroutines, or Compose — the view model executes each decision verbatim.
 *
 * Semantics mirrored exactly from the pre-extraction view model:
 * - remembered: drop the first card, append to removed, processedCardsCount +1,
 *   one RECALL-success decision with key GOOD:<ordinal>.
 * - forgot: drop the first card, re-queue it at the END, needsReview +1, no
 *   processedCardsCount change, one RECALL-fail decision with key AGAIN:<ordinal>.
 * - session ends when the visible list becomes empty.
 */
class WordSessionEngine(private val sessionStartMs: Long) {

    /** One durable-tap decision for the view model to execute. */
    data class RecallDecision(
        val wordId: Int,
        val correct: Boolean,
        val idempotencyKey: String,
        val sessionId: String,
    )

    /** Everything the view model needs after one swipe. */
    data class SwipeOutcome(
        val recall: RecallDecision,
        val visibleCards: List<WordMaster>,
        val removedCards: List<WordMaster>,
        val currentWord: WordMaster?,
        val processedCardsCount: Int,
        val needsReviewCount: Int,
        val sessionFinished: Boolean,
    )

    /** Termination facts for the view model to log on clear/exit (Step 3b). */
    data class SessionCompletion(
        val durationMs: Long,
        val cardsSwiped: Int,
        val needsReviewCount: Int,
        val cardsRemaining: Int,
    )

    private val visible = mutableListOf<WordMaster>()
    private val removed = mutableListOf<WordMaster>()
    private val tapOrdinals = HashMap<Int, Int>()
    private var processedCardsCount = 0
    var needsReviewCount = 0
        private set

    /** Session id = start timestamp, exactly as the view model used to build it. */
    val sessionId: String get() = sessionStartMs.toString()

    val cardsSwiped: Int get() = processedCardsCount + needsReviewCount

    /** Resets and loads the deck (mirrors loadDataInternal's session reset). */
    fun start(cards: List<WordMaster>) {
        visible.clear()
        visible.addAll(cards)
        removed.clear()
        tapOrdinals.clear()
        processedCardsCount = 0
        needsReviewCount = 0
    }

    fun swipeRemembered(word: WordMaster, nowMs: Long): SwipeOutcome {
        val remaining = visible.drop(1)
        visible.clear()
        visible.addAll(remaining)
        removed.add(word)
        processedCardsCount += 1
        return SwipeOutcome(
            recall = RecallDecision(
                wordId = word.id,
                correct = true,
                idempotencyKey = tapKey(word.id, "GOOD"),
                sessionId = sessionId,
            ),
            visibleCards = visible.toList(),
            removedCards = removed.toList(),
            currentWord = visible.firstOrNull(),
            processedCardsCount = processedCardsCount,
            needsReviewCount = needsReviewCount,
            sessionFinished = visible.isEmpty(),
        )
    }

    fun swipeForgot(word: WordMaster, nowMs: Long): SwipeOutcome {
        val remaining = visible.drop(1) + word
        visible.clear()
        visible.addAll(remaining)
        needsReviewCount += 1
        return SwipeOutcome(
            recall = RecallDecision(
                wordId = word.id,
                correct = false,
                idempotencyKey = tapKey(word.id, "AGAIN"),
                sessionId = sessionId,
            ),
            visibleCards = visible.toList(),
            removedCards = removed.toList(),
            currentWord = visible.firstOrNull(),
            processedCardsCount = processedCardsCount,
            needsReviewCount = needsReviewCount,
            sessionFinished = false,
        )
    }

    /**
     * Exit decision (Step 3b): the welcome-screen reset keeps the interrupted
     * session's id — durable keys stay attributable to the original session —
     * while counters restart clean in the fresh instance.
     */
    fun restarted(): WordSessionEngine = WordSessionEngine(sessionStartMs)

    /**
     * Termination decision (Step 3b): null when the session produced no taps
     * or its start timestamp is unusable — exactly the gate the view model
     * used to apply before logging completion.
     */
    fun completionFor(nowMs: Long, cardsRemaining: Int): SessionCompletion? {
        if (sessionStartMs <= 0 || cardsSwiped == 0) return null
        return SessionCompletion(
            durationMs = nowMs - sessionStartMs,
            cardsSwiped = cardsSwiped,
            needsReviewCount = needsReviewCount,
            cardsRemaining = cardsRemaining,
        )
    }

    /** Duration helper: the view model stamps now; the engine only subtracts. */
    fun durationMs(nowMs: Long): Long = nowMs - sessionStartMs

    /**
     * Production (spelling practice) taps share the same key space and session id
     * as recall taps — exactly as the pre-extraction view model did.
     */
    fun tapProductionKey(wordId: Int, correct: Boolean): String =
        tapKey(wordId, if (correct) "GOOD" else "AGAIN")

    private fun tapKey(wordId: Int, outcome: String): String {
        val ordinal = (tapOrdinals[wordId] ?: 0) + 1
        tapOrdinals[wordId] = ordinal
        return "$sessionStartMs:$wordId:$outcome:$ordinal"
    }
}
