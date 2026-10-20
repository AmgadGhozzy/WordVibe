package com.venom.domain.model

import androidx.compose.runtime.Immutable
import com.venom.domain.model.CefrLevel.A1
import com.venom.domain.model.CefrLevel.A2
import com.venom.domain.model.CefrLevel.B1
import com.venom.domain.model.CefrLevel.B2
import com.venom.domain.model.CefrLevel.C1
import com.venom.domain.model.CefrLevel.C2

/**
 * Data class representing a word with all its linguistic information.
 */
@Immutable
data class WordMaster(
    val id: Int,
    val wordEn: String,
    val pos: String, // adj, adv, conj, det, excl, modal, noun, num, prep, pron, verb
    val cefrLevel: CefrLevel,
    val fromOxford: Int, // 0 or 1
    val rank: Int, // Global rank (1-6500+)
    val frequency: Int, // 1-6 (1=Essential, 6=Advanced)
    val difficultyScore: Int, // 1-10 (Calculated)

    val phoneticUs: String,
    val phoneticUk: String?,
    val phoneticAr: String, // Arabic script with Tashkeel
    val translit: String, // Romanized Arabic
    val syllabify: String?, // Using bullet separator (•)

    val definitionEn: String,
    val definitionAr: String?,
    val usageNote: String?,
    val category: String?, // Thematic folder
    val primarySense: String?, // Sense Anchor
    val semanticTags: List<String>, // Hashtags (e.g., #Business, #Finance)
    val register: String, // Formal, Informal, Neutral

    val mnemonicAr: String?, // Memory hook in Arabic

    val wordFamily: WordFamily,
    val synonyms: List<String>,
    val antonyms: List<String>,
    val examples: Map<CefrLevel, String>,
    val collocations: List<String>,
    val relatedWords: RelatedWords,

    val arabicAr: String,
    val frenchFr: String? = null,
    val germanDe: String? = null,
    val spanishEs: String? = null,
    val chineseZh: String? = null,
    val russianRu: String? = null,
    val portuguesePt: String? = null,
    val japaneseJa: String? = null,
    val italianIt: String? = null,
    val turkishTr: String? = null,

    val unitId: String? = null,

    // Learning contract fields (frozen): retrieval/production targets, provenance, review
    val recallTarget: String? = null, // Gapped sentence, contains blank marker
    val productionExpected: String? = null, // Explicit expected form (never inferred)
    val productionVariants: List<String> = emptyList(), // Accepted alternative forms
    val sourceRef: String? = null,
    val ieltsTopic: String? = null, // Closed 10-topic list
    val ieltsRelevance: String? = null, // Non-empty rationale sentence
    val reviewer: String? = null,
    val reviewedAt: String? = null, // YYYY-MM-DD
    val checklist: List<String> = emptyList() // Attested acceptance rules
)

enum class CefrLevel(val displayName: String) {
    A1("A1"),
    A2("A2"),
    B1("B1"),
    B2("B2"),
    C1("C1"),
    C2("C2")
}

@Immutable
data class WordFamily(
    val noun: String? = null,
    val verb: String? = null,
    val adj: String? = null,
    val adv: String? = null
)

@Immutable
data class RelatedWords(
    val english: List<String>,
    val arabic: List<String>
)

@Immutable
data class LanguageOption(
    val langName: String,
    val translation: String?
)


fun getFrequencyLabel(level: Int): String = when (level) {
    1 -> "Very Common"
    2 -> "Very Common"
    3 -> "Common"
    4 -> "Average"
    5 -> "Uncommon"
    6 -> "Rare"
    7 -> "Obscure"
    else -> "Unknown"
}

fun getLanguageOptions(word: WordMaster): List<LanguageOption> = listOfNotNull(
    LanguageOption("Arabic", word.arabicAr),
    word.frenchFr?.let { LanguageOption("French", it) },
    word.germanDe?.let { LanguageOption("German", it) },
    word.spanishEs?.let { LanguageOption("Spanish", it) },
    word.chineseZh?.let { LanguageOption("Chinese", it) },
    word.russianRu?.let { LanguageOption("Russian", it) },
    word.portuguesePt?.let { LanguageOption("Portuguese", it) },
    word.japaneseJa?.let { LanguageOption("Japanese", it) },
    word.italianIt?.let { LanguageOption("Italian", it) },
    word.turkishTr?.let { LanguageOption("Turkish", it) }
)
