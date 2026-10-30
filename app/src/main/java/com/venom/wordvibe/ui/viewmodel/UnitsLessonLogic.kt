package com.venom.wordvibe.ui.viewmodel

import com.venom.domain.model.WordMaster

/**
 * Deterministic, read-only projections for the unit preview lesson (ADR-0011
 * deep-dive). Pure Kotlin — no Android types, no repository calls, no writes.
 */

/**
 * Usable lesson words for [topic]: topic match (ignoreCase) with both
 * definitionEn and arabicAr present (the same usability contract as
 * getUnitsStatusForUser), ordered rank-ascending regardless of input order.
 */
internal fun usableWordsForTopic(words: List<WordMaster>, topic: String): List<WordMaster> =
    words
        .filter {
            it.ieltsTopic.equals(topic, ignoreCase = true) &&
                it.definitionEn.isNotBlank() &&
                it.arabicAr.isNotBlank()
        }
        .sortedBy { it.rank }

/** Complete-the-sentence challenge extracted from the frozen recallTarget. */
data class RecallLesson(
    val gapped: String,
    val hintFirstLetter: String,
    val answer: String,
)

/**
 * recallTarget is contractually a gapped sentence carrying the "___" marker
 * (CorpusSeeder acceptance rule). Null when the word has no usable target.
 * Answer is always the actual word — the screen only reveals it, never grades.
 */
internal fun recallLessonFor(word: WordMaster): RecallLesson? {
    val target = word.recallTarget ?: return null
    if ("___" !in target) return null
    return RecallLesson(
        gapped = target,
        hintFirstLetter = word.wordEn.take(1).uppercase(),
        answer = word.wordEn,
    )
}

/** Untracked synonym pick: exactly three distinct options, no randomness. */
data class SynonymChallenge(
    val wordId: Int,
    val wordEn: String,
    val correct: String,
    val options: List<String>,
)

/**
 * Correct option = the word's first non-blank synonym. Distractors come from
 * the neighbors of the same ranked list (rotation from [index]); alphabetical
 * ordering keeps the layout stable. Null when fewer than two distinct
 * distractors exist, so only answerable challenges are shown.
 */
internal fun synonymChallengeFor(words: List<WordMaster>, index: Int): SynonymChallenge? {
    val word = words.getOrNull(index) ?: return null
    val correct = word.synonyms.firstOrNull { it.isNotBlank() } ?: return null
    val distractors = ArrayList<String>(2)
    var i = (index + 1) % words.size
    repeat(words.size) {
        if (distractors.size < 2) {
            val candidate = words[i].synonyms.firstOrNull { it.isNotBlank() }
            if (candidate != null &&
                !candidate.equals(correct, ignoreCase = true) &&
                distractors.none { it.equals(candidate, ignoreCase = true) }
            ) {
                distractors.add(candidate)
            }
            i = (i + 1) % words.size
        }
    }
    if (distractors.size < 2) return null
    return SynonymChallenge(
        wordId = word.id,
        wordEn = word.wordEn,
        correct = correct,
        options = (listOf(correct) + distractors).sortedBy { it.lowercase() },
    )
}
