package com.venom.data.corpus

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Ticket 02 golden contracts: the pilot corpus asset satisfies the frozen per-word
 * acceptance contract. Reads the real asset file (unit-test working dir = module dir).
 */
class CorpusContractTest {

    private fun loadAsset(): String {
        val file = File("src/main/assets/words_pilot.json")
        assertTrue(file.exists(), "pilot corpus asset must exist at src/main/assets/words_pilot.json")
        return file.readText()
    }

    @Test
    fun `every pilot word passes the acceptance contract`() {
        val violations = validateCorpus(loadAsset())

        assertTrue(
            violations.isEmpty(),
            "corpus violations:\n" + violations.take(10).joinToString("\n")
        )
    }

    @Test
    fun `broken entry fails validation loudly`() {
        val bad = """
            {"words": [{
                "id": 1, "wordEn": "beautiful", "pos": "adjective",
                "cefrLevel": "B2", "rank": 400, "frequency": 2,
                "phoneticUs": "ˈbjuːtɪfəl", "phoneticAr": "بْيُوتِفُل", "translit": "Jamīl",
                "definitionEn": "Pleasing the senses.",
                "arabicAr": "", "examples": {"B2": "It is beautiful."},
                "collocations": [], "synonyms": [],
                "recallTarget": "", "productionExpected": "",
                "productionVariants": [], "sourceRef": "",
                "ieltsTopic": "Dreams", "ieltsRelevance": "",
                "reviewer": "", "reviewedAt": "yesterday"
            }]}
        """.trimIndent()

        assertTrue(
            validateCorpus(bad).isNotEmpty(),
            "an entry with empty meaning, no collocations/synonyms, bad topic and bad date must fail"
        )
    }
}
