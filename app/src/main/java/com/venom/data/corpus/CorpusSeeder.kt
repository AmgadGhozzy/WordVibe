package com.venom.data.corpus

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.venom.data.local.PreferencesKeys
import com.venom.domain.model.CefrLevel
import com.venom.domain.model.RelatedWords
import com.venom.domain.model.WordFamily
import com.venom.domain.model.WordMaster
import com.venom.domain.repo.IWordMasterRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Closed IELTS topic list (frozen corpus contract). */
val IELTS_TOPICS = setOf(
    "Education", "Work", "Environment", "Health", "Technology",
    "Society", "Culture", "Economy", "Science", "Travel"
)

private val DATE_RE = Regex("""\d{4}-\d{2}-\d{2}""")
private val LEMMA_RE = Regex("""^[A-Za-z'\-]+$""")
// \p{IsArabic} (script property) needs API 26+. This union covers every Arabic
// script block (base, Extended-A, presentation forms) on all supported APIs —
// same acceptance as the script property, pinned by CorpusArabicScriptTest.
private val ARABIC_SCRIPT = Regex("""[\p{InArabic}ࢠ-ࣿﭐ-﷿ﹰ-﻿]""")

class CorpusValidationException(violations: List<String>) :
    IllegalStateException("corpus rejected:\n" + violations.joinToString("\n"))

private fun JsonObject.stringField(key: String): String =
    this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull ?: ""

private fun JsonObject.intField(key: String, default: Int = 0): Int =
    this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull ?: default

private fun JsonObject.stringListField(key: String): List<String> =
    this[key]?.takeUnless { it is JsonNull }?.jsonArray?.mapNotNull { element ->
        element.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull
    } ?: emptyList()

private fun JsonObject.objectField(key: String): JsonObject? =
    this[key]?.takeUnless { it is JsonNull }?.jsonObject

private fun JsonObject.arrayField(key: String): JsonArray? =
    this[key]?.takeUnless { it is JsonNull }?.jsonArray

/** A string array field holding >= 1 non-empty entry. */
private fun JsonObject.nonEmptyStringArray(key: String): Boolean =
    arrayField(key)?.let { arr ->
        arr.isNotEmpty() && arr.all { el ->
            el.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull?.isNotBlank() == true
        }
    } == true

/**
 * Validates a pilot corpus JSON document against the frozen per-word acceptance contract.
 * Returns the list of violations (empty = accepted). Pure function:
 * kotlinx.serialization tree API only, so it runs on plain JVM unit tests.
 */
fun validateCorpus(json: String): List<String> {
    val violations = mutableListOf<String>()
    val root = try {
        Json.parseToJsonElement(json).jsonObject
    } catch (e: Exception) {
        return listOf("document is not valid JSON: ${e.message}")
    }
    val words = root.arrayField("words")
    if (words == null || words.isEmpty()) {
        return listOf("document has no non-empty 'words' array")
    }
    val seen = mutableSetOf<String>()
    val cefrNames = CefrLevel.entries.map { it.name }
    words.forEachIndexed { i, element ->
        val tag = "word[$i]"
        val obj = try {
            element.jsonObject
        } catch (_: Exception) {
            violations += "$tag: not a JSON object"
            return@forEachIndexed
        }
        val wordEn = obj.stringField("wordEn")
        val label = if (wordEn.isNotBlank()) "$tag ($wordEn)" else tag

        if (!obj.containsKey("id")) violations += "$label: missing id"
        if (wordEn.isBlank()) {
            violations += "$label: wordEn must be non-empty"
        } else {
            if (!LEMMA_RE.matches(wordEn)) {
                violations += "$label: illegal lemma (letters/hyphen/apostrophe only)"
            }
            if (!seen.add(wordEn.lowercase())) {
                violations += "$label: duplicate word (case-insensitive)"
            }
        }
        if (obj.stringField("pos").isBlank()) violations += "$label: pos must be non-empty"
        if (obj.stringField("cefrLevel") !in cefrNames) {
            violations += "$label: cefrLevel must be one of A1..C2"
        }
        if (obj.intField("rank") < 1) violations += "$label: rank must be >= 1"
        if (obj.intField("frequency") !in 1..6) violations += "$label: frequency must be 1..6"
        if (obj.stringField("phoneticUs").isBlank()) violations += "$label: phoneticUs required"
        if (obj.stringField("phoneticAr").isBlank()) violations += "$label: phoneticAr required"
        val arabic = obj.stringField("arabicAr")
        if (arabic.isBlank()) {
            violations += "$label: arabicAr must be non-empty"
        } else if (!ARABIC_SCRIPT.containsMatchIn(arabic)) {
            violations += "$label: arabicAr must be Arabic script"
        }
        if (obj.stringField("definitionEn").isBlank()) {
            violations += "$label: definitionEn must be non-empty"
        }
        val examples = obj.objectField("examples")
        if (examples == null || examples.isEmpty()) {
            violations += "$label: at least one example required"
        } else {
            if (examples.keys.any { it.uppercase() !in cefrNames }) {
                violations += "$label: example keys must be CEFR levels"
            }
            val text = examples.keys.joinToString(" ") { examples.stringField(it) }
            if (wordEn.isNotBlank() && !usesWordForm(text, wordEn)) {
                violations += "$label: no example uses a valid form of the word"
            }
        }
        if (!obj.nonEmptyStringArray("collocations")) {
            violations += "$label: collocations must hold >= 1 non-empty entry"
        }
        if (!obj.nonEmptyStringArray("synonyms")) {
            violations += "$label: synonyms must hold >= 1 non-empty entry"
        }
        val family = obj.objectField("wordFamily")
        if (family == null || listOf("noun", "verb", "adj", "adv").none { key ->
                family.stringField(key).isNotBlank()
            }
        ) {
            violations += "$label: wordFamily must hold >= 1 non-empty member"
        }
        val recallTarget = obj.stringField("recallTarget")
        if (recallTarget.isBlank() || "___" !in recallTarget) {
            violations += "$label: recallTarget must be a gapped sentence (___ marker)"
        }
        if (obj.stringField("productionExpected").isBlank()) {
            violations += "$label: productionExpected must be non-empty"
        }
        if (obj.arrayField("productionVariants") == null) {
            violations += "$label: productionVariants array must be present (may be empty)"
        }
        if (obj.stringField("sourceRef").isBlank()) violations += "$label: sourceRef required"
        if (obj.stringField("ieltsTopic") !in IELTS_TOPICS) {
            violations += "$label: ieltsTopic must be one of $IELTS_TOPICS"
        }
        if (obj.stringField("ieltsRelevance").isBlank()) {
            violations += "$label: ieltsRelevance rationale required"
        }
        if (obj.stringField("reviewer").isBlank()) violations += "$label: reviewer sign-off required"
        if (!obj.nonEmptyStringArray("checklist")) {
            violations += "$label: checklist must attest >= 1 reviewed rule"
        }
        if (!DATE_RE.matches(obj.stringField("reviewedAt"))) {
            violations += "$label: reviewedAt must be YYYY-MM-DD"
        }
    }
    return violations
}

/** Approximation (documented): some token shares a stem-length prefix with the word. */
private fun usesWordForm(text: String, wordEn: String): Boolean {
    val need = minOf(4, wordEn.length)
    val target = wordEn.lowercase()
    return text.lowercase().split(Regex("[^a-z']+")).any { token ->
        token.length >= need && token.commonPrefixWith(target).length >= need
    }
}

/** Parses a validated corpus document into domain words. Throws on violations. */
fun parseCorpus(json: String): List<WordMaster> {
    val violations = validateCorpus(json)
    if (violations.isNotEmpty()) throw CorpusValidationException(violations)
    val words = Json.parseToJsonElement(json).jsonObject.getValue("words").jsonArray
    return words.map { element ->
        val obj = element.jsonObject
        fun optStr(key: String): String? = obj.stringField(key).takeIf { it.isNotEmpty() }
        val examplesObj = obj.objectField("examples") ?: JsonObject(emptyMap())
        val family = obj.objectField("wordFamily")
        fun familyField(key: String): String? =
            family?.stringField(key)?.takeIf { it.isNotEmpty() }
        val related = obj.objectField("relatedWords")
        WordMaster(
            id = obj.intField("id"),
            wordEn = obj.stringField("wordEn"),
            pos = obj.stringField("pos"),
            cefrLevel = CefrLevel.valueOf(obj.stringField("cefrLevel")),
            fromOxford = obj.intField("fromOxford"),
            rank = obj.intField("rank"),
            frequency = obj.intField("frequency"),
            difficultyScore = obj.intField("difficultyScore", 5),
            phoneticUs = obj.stringField("phoneticUs"),
            phoneticUk = optStr("phoneticUk"),
            phoneticAr = obj.stringField("phoneticAr"),
            translit = obj.stringField("translit"),
            syllabify = optStr("syllabify"),
            definitionEn = obj.stringField("definitionEn"),
            definitionAr = optStr("definitionAr"),
            usageNote = optStr("usageNote"),
            category = optStr("category"),
            primarySense = optStr("primarySense"),
            semanticTags = obj.stringListField("semanticTags"),
            register = obj.stringField("register").takeIf { it.isNotEmpty() } ?: "Neutral",
            mnemonicAr = optStr("mnemonicAr"),
            wordFamily = WordFamily(
                noun = familyField("noun"),
                verb = familyField("verb"),
                adj = familyField("adj"),
                adv = familyField("adv")
            ),
            synonyms = obj.stringListField("synonyms"),
            antonyms = obj.stringListField("antonyms"),
            examples = examplesObj.keys.associate { key ->
                CefrLevel.valueOf(key.uppercase()) to examplesObj.stringField(key)
            },
            collocations = obj.stringListField("collocations"),
            relatedWords = RelatedWords(
                english = related?.stringListField("en") ?: emptyList(),
                arabic = related?.stringListField("ar") ?: emptyList()
            ),
            arabicAr = obj.stringField("arabicAr"),
            frenchFr = optStr("frenchFr"),
            germanDe = optStr("germanDe"),
            spanishEs = optStr("spanishEs"),
            chineseZh = optStr("chineseZh"),
            russianRu = optStr("russianRu"),
            portuguesePt = optStr("portuguesePt"),
            japaneseJa = optStr("japaneseJa"),
            italianIt = optStr("italianIt"),
            turkishTr = optStr("turkishTr"),
            unitId = optStr("unitId"),
            recallTarget = obj.stringField("recallTarget"),
            productionExpected = obj.stringField("productionExpected"),
            productionVariants = obj.stringListField("productionVariants"),
            sourceRef = obj.stringField("sourceRef"),
            ieltsTopic = obj.stringField("ieltsTopic"),
            ieltsRelevance = obj.stringField("ieltsRelevance"),
            reviewer = obj.stringField("reviewer"),
            reviewedAt = obj.stringField("reviewedAt"),
            checklist = obj.stringListField("checklist")
        )
    }
}

/** Persisted first-launch flag, abstracted for tests. */
interface SeedFlagStore {
    suspend fun isSeeded(): Boolean
    suspend fun setSeeded()
}

class DataStoreSeedFlag(private val dataStore: DataStore<Preferences>) : SeedFlagStore {
    override suspend fun isSeeded(): Boolean =
        dataStore.data.map { it[PreferencesKeys.SEED_COMPLETED] ?: false }.first()

    override suspend fun setSeeded() {
        dataStore.edit { it[PreferencesKeys.SEED_COMPLETED] = true }
    }
}

/**
 * Idempotent first-launch seeder: validates the bundled corpus, inserts it once, sets the
 * flag. Returns the inserted count (0 = skipped). A populated store is never reseeded.
 */
class CorpusSeeder(
    private val wordRepo: IWordMasterRepository,
    private val flags: SeedFlagStore
) {
    suspend fun seedIfNeeded(json: String): Int = withContext(Dispatchers.IO) {
        if (flags.isSeeded()) return@withContext 0
        if (wordRepo.getWordCount() > 0) {
            flags.setSeeded()
            return@withContext 0
        }
        val words = parseCorpus(json)
        wordRepo.insertWords(words)
        flags.setSeeded()
        words.size
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CorpusSeederEntryPoint {
    fun wordRepository(): IWordMasterRepository
    fun dataStore(): DataStore<Preferences>
}
