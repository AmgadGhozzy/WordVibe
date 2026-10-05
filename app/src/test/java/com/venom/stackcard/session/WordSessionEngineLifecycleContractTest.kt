package com.venom.stackcard.session

import com.venom.domain.model.CefrLevel
import com.venom.domain.model.WordFamily
import com.venom.domain.model.WordMaster
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Step 3b lifecycle contracts on the pure engine: the welcome-screen restart
 * keeps the interrupted session's id with clean counters, and the completion
 * decision carries the exact facts the view model used to compute inline.
 * The VM characterization suite (WordMasterySessionLifecycleContractTest)
 * pins the same decisions through the view model.
 */
class WordSessionEngineLifecycleContractTest {

    private val startMs = 1_000_000L

    private fun word(id: Int) = WordMaster(
        id = id,
        wordEn = "w$id",
        pos = "verb",
        cefrLevel = CefrLevel.B1,
        fromOxford = 1,
        rank = 1000 + id,
        frequency = 3,
        difficultyScore = 5,
        phoneticUs = "t",
        phoneticUk = null,
        phoneticAr = "كلمة",
        translit = "t",
        syllabify = null,
        definitionEn = "d",
        definitionAr = null,
        usageNote = null,
        category = null,
        primarySense = null,
        semanticTags = emptyList(),
        register = "Neutral",
        mnemonicAr = null,
        wordFamily = WordFamily(),
        synonyms = emptyList(),
        antonyms = emptyList(),
        examples = emptyMap(),
        collocations = emptyList(),
        relatedWords = com.venom.domain.model.RelatedWords(emptyList(), emptyList()),
        arabicAr = "كلمة",
    )

    private fun engine(vararg words: WordMaster) =
        WordSessionEngine(sessionStartMs = startMs).also { it.start(words.toList()) }

    @Test
    fun `restarted keeps the session id so durable keys stay attributable`() {
        val e = engine(word(1))
        e.swipeForgot(word(1), nowMs = startMs + 1)

        val fresh = e.restarted()

        assertEquals(e.sessionId, fresh.sessionId)
        fresh.start(listOf(word(9)))
        val out = fresh.swipeForgot(word(9), nowMs = startMs + 9)
        assertEquals(
            "$startMs:9:AGAIN:1", out.recall.idempotencyKey,
            "fresh instance, same session id, ordinal restarts at 1"
        )
    }

    @Test
    fun `restarted resets counters for a fresh keyspace`() {
        val e = engine(word(1), word(2))
        e.swipeRemembered(word(1), nowMs = startMs + 1)
        e.swipeForgot(word(2), nowMs = startMs + 2)

        val fresh = e.restarted()

        assertEquals(0, fresh.cardsSwiped)
        assertEquals(0, fresh.needsReviewCount)
    }

    @Test
    fun `restarted from a null predecessor is the view model's fallback`() {
        // Mirrors resetToWelcome's ?: branch — the VM owns the fallback; this
        // pins the engine constructor it falls back to.
        val fresh = WordSessionEngine(sessionStartMs = 123L)
        assertEquals("123", fresh.sessionId)
    }

    @Test
    fun `completion is null for an untouched session`() {
        val e = engine(word(1))

        assertNull(e.completionFor(nowMs = startMs + 50, cardsRemaining = 1))
    }

    @Test
    fun `completion is null when the session start is not positive`() {
        val e = WordSessionEngine(sessionStartMs = 0L).also { it.start(listOf(word(1))) }
        e.swipeRemembered(word(1), nowMs = 5)

        assertNull(e.completionFor(nowMs = 50, cardsRemaining = 0))
    }

    @Test
    fun `completion carries duration, counters and remaining cards`() {
        val e = engine(word(1), word(2), word(3), word(4))
        e.swipeRemembered(word(1), nowMs = startMs + 10)
        e.swipeForgot(word(2), nowMs = startMs + 20)

        val completion = e.completionFor(nowMs = startMs + 50, cardsRemaining = 2)
        assertNotNull(completion)

        assertEquals(50L, completion!!.durationMs)
        assertEquals(2, completion.cardsSwiped)
        assertEquals(1, completion.needsReviewCount)
        assertEquals(2, completion.cardsRemaining)
    }
}
