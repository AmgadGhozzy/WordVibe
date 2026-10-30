package com.venom.wordvibe.ui.viewmodel

import com.venom.domain.model.CefrLevel
import com.venom.domain.model.WordFamily
import com.venom.domain.model.WordMaster
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UnitsLessonLogicTest {

    private fun word(
        id: Int,
        en: String = "w$id",
        rank: Int = id,
        topic: String = "Education",
        definitionEn: String = "definition of $en",
        arabicAr: String = "عربي $id",
        synonyms: List<String> = emptyList(),
        recallTarget: String? = null,
    ): WordMaster = WordMaster(
        id = id,
        wordEn = en,
        pos = "noun",
        cefrLevel = CefrLevel.B1,
        fromOxford = 1,
        rank = rank,
        frequency = 3,
        difficultyScore = 5,
        phoneticUs = "/$en/",
        phoneticUk = null,
        phoneticAr = en,
        translit = en,
        syllabify = null,
        definitionEn = definitionEn,
        definitionAr = null,
        usageNote = null,
        category = null,
        primarySense = null,
        semanticTags = emptyList(),
        register = "Neutral",
        mnemonicAr = null,
        wordFamily = WordFamily(noun = en),
        synonyms = synonyms,
        antonyms = emptyList(),
        examples = emptyMap(),
        collocations = emptyList(),
        relatedWords = com.venom.domain.model.RelatedWords(emptyList(), emptyList()),
        arabicAr = arabicAr,
        recallTarget = recallTarget,
        ieltsTopic = topic,
    )

    @Test
    fun `filters by topic and usability without touching other topics`() {
        val words = listOf(
            word(1),
            word(2, topic = "Work"),
            word(3, definitionEn = "   ", arabicAr = "مسمى"), // absent definitionEn
            word(4, arabicAr = ""), // absent arabicAr
            word(5),
        )
        val out = usableWordsForTopic(words, "education")
        assertEquals(listOf("w1", "w5"), out.map { it.wordEn })
    }

    @Test
    fun `output stays rank-ascending even when input is scrambled`() {
        val words = listOf(word(3, en = "c", rank = 30), word(1, en = "a", rank = 10), word(2, en = "b", rank = 20))
        val out = usableWordsForTopic(words, "Education")
        assertEquals(listOf("a", "b", "c"), out.map { it.wordEn })
    }

    @Test
    fun `recall lesson extracts gap hint and actual word`() {
        val w = word(9, en = "obtain", recallTarget = "She managed to ___ her degree last year.")
        val lesson = recallLessonFor(w)!!
        assertEquals("She managed to ___ her degree last year.", lesson.gapped)
        assertEquals("O", lesson.hintFirstLetter)
        assertEquals("obtain", lesson.answer)
    }

    @Test
    fun `recall lesson is null without target or blank marker`() {
        assertNull(recallLessonFor(word(1)))
        assertNull(recallLessonFor(word(2, recallTarget = "no gap here")))
    }

    @Test
    fun `synonym challenge builds three distinct deterministic options`() {
        val words = listOf(
            word(1, synonyms = listOf("acquire")),
            word(2, synonyms = listOf("gain")),
            word(3, synonyms = listOf("attain")),
        )
        val challenge = synonymChallengeFor(words, 0)!!
        assertEquals(1, challenge.wordId)
        assertEquals("acquire", challenge.correct)
        assertEquals(3, challenge.options.size)
        assertEquals(setOf("acquire", "gain", "attain"), challenge.options.toSet())
        assertEquals(challenge.options, challenge.options.sortedBy { it.lowercase() })
    }

    @Test
    fun `synonym challenge is null when no full distinct option set exists`() {
        assertNull(synonymChallengeFor(listOf(word(1, synonyms = listOf("acquire"))), 0))
        assertNull(
            synonymChallengeFor(
                listOf(word(1, synonyms = listOf("acquire")), word(2, synonyms = emptyList())),
                0,
            ),
        )
        assertNull(synonymChallengeFor(emptyList(), 0))
    }
}
