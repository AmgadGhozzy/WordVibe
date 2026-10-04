package com.venom.data.corpus

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Phase 2 closure: the API-24-safe Arabic matcher must accept exactly what the
 * script property accepted — base letters, tashkeel, Persian/Urdu extensions and
 * presentation forms — and reject non-Arabic script. Also pins that the validator
 * still demands a gapped recallTarget for every word (the provide/ADR-0005 check).
 */
class CorpusArabicScriptTest {

    @Test
    fun `arabic matcher accepts every arabic block`() {
        val samples = listOf(
            "كتاب",              // base letters
            "مُعَلِّم",           // tashkeel
            "گچپژ",              // Persian/Urdu extensions (U+06xx)
            "كِتَابٍ ٱلْعَرَبِيَّة",   // dagger vowels + wasla
        )
        for (sample in samples) {
            assertEquals(
                true,
                Regex("""[\p{InArabic}ࢠ-ࣿﭐ-﷿ﹰ-﻿]""").containsMatchIn(sample),
                "must accept: $sample"
            )
        }
    }

    @Test
    fun `arabic matcher rejects non-arabic script`() {
        for (sample in listOf("provide", "hello 123", "")) {
            assertEquals(
                false,
                Regex("""[\p{InArabic}ࢠ-ࣿﭐ-﷿ﹰ-﻿]""").containsMatchIn(sample),
                "must reject: $sample"
            )
        }
    }

    @Test
    fun `validator still demands a gapped recall target`() {
        val corpus = """{"words": [{
            "id": 1, "wordEn": "provide", "pos": "verb", "cefrLevel": "B1",
            "rank": 600, "frequency": 3, "phoneticUs": "p", "phoneticAr": "بروفايد",
            "arabicAr": "يوفر", "definitionEn": "d",
            "examples": {"B1": "The hotel provides free breakfast."},
            "collocations": ["provide help"], "synonyms": ["supply"],
            "wordFamily": {"verb": "provide"},
            "recallTarget": "The hotel ______ free breakfast.",
            "productionExpected": "provide", "productionVariants": [],
            "sourceRef": "s", "ieltsTopic": "Education", "ieltsRelevance": "r",
            "reviewer": "v", "reviewedAt": "2026-09-24", "checklist": ["lemma"]
        }]}"""
        // with recallTarget: accepted
        assertEquals(emptyList<String>(), validateCorpus(corpus))
        // recallTarget removed: rejected loudly
        val broken = corpus.replace("\"recallTarget\": \"The hotel ______ free breakfast.\",", "")
        assert(validateCorpus(broken).any { it.contains("recallTarget") })
    }
}
