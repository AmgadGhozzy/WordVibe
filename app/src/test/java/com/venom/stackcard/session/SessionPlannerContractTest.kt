package com.venom.stackcard.session

import com.venom.domain.model.CefrLevel
import com.venom.domain.model.WordFamily
import com.venom.domain.model.WordMaster
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Step 3b contracts for the pure session planner: due words serve first,
 * then new words fill the session limit. The composition was migrated
 * verbatim from loadSrsWords — the VM characterization suite
 * (WordMasterySessionLifecycleContractTest) pins the same decision through
 * the real repositories.
 */
class SessionPlannerContractTest {

    private fun word(id: Int) = WordMaster(
        id = id,
        wordEn = "w$id",
        pos = "verb",
        cefrLevel = CefrLevel.B1,
        fromOxford = 1,
        rank = id,
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

    private val limit = 5

    @Test
    fun `session limit migrated from the inline constant is 15`() {
        assertEquals(15, SessionPlanner.SESSION_LIMIT)
    }

    @Test
    fun `due words serve first, then new words`() {
        val deck = SessionPlanner.plan(
            due = listOf(word(1), word(2)),
            newWords = listOf(word(9), word(8)),
            limit = limit
        )
        assertEquals(listOf(1, 2, 9, 8), deck.map { it.id })
    }

    @Test
    fun `new words fill the remaining limit`() {
        val deck = SessionPlanner.plan(
            due = listOf(word(1), word(2)),
            newWords = listOf(word(7), word(8), word(9), word(10)),
            limit = limit
        )
        assertEquals(listOf(1, 2, 7, 8, 9), deck.map { it.id })
    }

    @Test
    fun `a full due list admits no new words`() {
        val deck = SessionPlanner.plan(
            due = (1..limit).map(::word),
            newWords = listOf(word(9), word(8)),
            limit = limit
        )
        assertEquals((1..limit).toList(), deck.map { it.id })
    }

    @Test
    fun `due beyond the limit is truncated`() {
        val deck = SessionPlanner.plan(
            due = (1..9).map(::word),
            newWords = listOf(word(20)),
            limit = limit
        )
        assertEquals((1..limit).toList(), deck.map { it.id })
    }

    @Test
    fun `no due words means new words only`() {
        val deck = SessionPlanner.plan(
            due = emptyList(),
            newWords = (10..20).map(::word),
            limit = limit
        )
        assertEquals((10..14).toList(), deck.map { it.id })
    }

    @Test
    fun `short new pool serves what exists`() {
        val deck = SessionPlanner.plan(
            due = listOf(word(1)),
            newWords = listOf(word(2)),
            limit = limit
        )
        assertEquals(listOf(1, 2), deck.map { it.id })
    }

    @Test
    fun `zero limit serves nothing`() {
        val deck = SessionPlanner.plan(
            due = listOf(word(1)),
            newWords = listOf(word(2)),
            limit = 0
        )
        assertEquals(emptyList<Int>(), deck.map { it.id })
    }
}
