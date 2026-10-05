package com.venom.stackcard.session

import com.venom.domain.model.CefrLevel
import com.venom.domain.model.WordFamily
import com.venom.domain.model.WordMaster
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Step 3 contracts for the pure session engine. These mirror the pre-extraction
 * view-model behavior line for line — the VM integration tests (real repos,
 * untouched) remain the behavior authority; this suite pins the engine in
 * isolation so the delegation can never drift.
 */
class WordSessionEngineContractTest {

    private val startMs = 1_000_000L

    private fun word(id: Int, en: String = "w$id") = WordMaster(
        id = id,
        wordEn = en,
        pos = "verb",
        cefrLevel = CefrLevel.B1,
        fromOxford = 1,
        rank = 1000 + id,
        frequency = 3,
        difficultyScore = 5,
        phoneticUs = "t",
        phoneticUk = null,
        phoneticAr = "اختبار",
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
        arabicAr = "يحصل على",
    )

    private fun engine(vararg words: WordMaster) =
        WordSessionEngine(sessionStartMs = startMs).also { it.start(words.toList()) }

    @Test
    fun `session id is the start timestamp`() {
        assertEquals(startMs.toString(), engine(word(1)).sessionId)
    }

    @Test
    fun `remember drops first card, appends to removed, and records one GOOD key`() {
        val e = engine(word(1), word(2))
        val out = e.swipeRemembered(word(1), nowMs = startMs + 5)

        assertEquals(1, out.recall.wordId)
        assertTrue(out.recall.correct)
        assertEquals("$startMs:1:GOOD:1", out.recall.idempotencyKey)
        assertEquals(listOf(word(2)), out.visibleCards)
        assertEquals(word(2), out.currentWord)
        assertEquals(1, out.processedCardsCount)
        assertEquals(0, out.needsReviewCount)
        assertFalse(out.sessionFinished)
        assertEquals(listOf(word(1)), out.removedCards)
    }

    @Test
    fun `forgot re-queues the card at the end and records one AGAIN key`() {
        val e = engine(word(1), word(2))
        val out = e.swipeForgot(word(1), nowMs = startMs + 5)

        assertFalse(out.recall.correct)
        assertEquals("$startMs:1:AGAIN:1", out.recall.idempotencyKey)
        assertEquals(listOf(word(2), word(1)), out.visibleCards, "forgot card re-queued at the end")
        assertEquals(word(2), out.currentWord)
        assertEquals(0, out.processedCardsCount, "forgot does not advance processed count")
        assertEquals(1, out.needsReviewCount)
        assertFalse(out.sessionFinished)
    }

    @Test
    fun `ordinals increase per word across outcomes`() {
        val e = engine(word(1))
        e.swipeForgot(word(1), nowMs = startMs + 1)   // :AGAIN:1
        e.swipeForgot(word(1), nowMs = startMs + 2)   // :AGAIN:2 (re-queued)
        val out = e.swipeRemembered(word(1), nowMs = startMs + 3) // :GOOD:3

        assertEquals("$startMs:1:GOOD:3", out.recall.idempotencyKey)
    }

    @Test
    fun `session finishes when the visible list empties`() {
        val e = engine(word(1))
        val out = e.swipeRemembered(word(1), nowMs = startMs + 5)

        assertTrue(out.sessionFinished)
        assertNull(out.currentWord)
        assertTrue(out.visibleCards.isEmpty())
    }

    @Test
    fun `forgot never finishes the session`() {
        val e = engine(word(1))
        val out = e.swipeForgot(word(1), nowMs = startMs + 5)

        assertFalse(out.sessionFinished)
        assertEquals(listOf(word(1)), out.visibleCards, "the card re-queues")
    }

    @Test
    fun `cards swiped counts both outcomes`() {
        val e = engine(word(1), word(2), word(3))
        e.swipeRemembered(word(1), nowMs = startMs + 1)
        e.swipeForgot(word(2), nowMs = startMs + 2)
        assertEquals(2, e.cardsSwiped)
    }

    @Test
    fun `start resets a previous session cleanly`() {
        val e = WordSessionEngine(sessionStartMs = startMs)
        e.start(listOf(word(1)))
        e.swipeRemembered(word(1), nowMs = startMs + 1)

        e.start(listOf(word(9)))
        val out = e.swipeRemembered(word(9), nowMs = startMs + 99)

        // New deck: ordinal restarts at 1 for the fresh session keyspace.
        assertEquals("$startMs:9:GOOD:1", out.recall.idempotencyKey)
        assertEquals(listOf(word(9)), out.removedCards, "remembered card joins the fresh removed list")
        assertEquals(1, out.processedCardsCount)
    }

    @Test
    fun `production taps share the key space with recall taps`() {
        val e = engine(word(1))
        val key = e.tapProductionKey(1, correct = true)

        assertEquals("$startMs:1:GOOD:1", key)
        val recall = e.swipeRemembered(word(1), nowMs = startMs + 5)
        assertEquals("$startMs:1:GOOD:2", recall.recall.idempotencyKey)
    }
}
