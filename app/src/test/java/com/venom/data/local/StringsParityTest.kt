package com.venom.data.local

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Ticket 18 (Batch 1): the English base strings (res/values/strings.xml) and the
 * Arabic strings (res/values-ar/strings.xml) must stay in lockstep — identical key
 * sets, identical ordered format placeholders, and no Arabic script leaking into the
 * English base. P5 lists any key in values-ar that unexpectedly lacks Arabic script.
 */
class StringsParityTest {

    private val enPath = "app/src/main/res/values/strings.xml"
    private val arPath = "app/src/main/res/values-ar/strings.xml"
    private val enPluralsPath = "app/src/main/res/values/plurals.xml"
    private val arPluralsPath = "app/src/main/res/values-ar/plurals.xml"

    private val stringEntry = Regex("""<string name="([^"]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
    private val pluralBlock = Regex("""<plurals name="([^"]+)">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
    private val pluralItem = Regex("""<item quantity="([^"]+)">(.*?)</item>""", RegexOption.DOT_MATCHES_ALL)
    private val placeholder = Regex("""%\d+\$[sd]|%[sd]""")
    private val arabicScript = Regex("""[\u0600-\u06FF]""")

    /** Keys whose value is a symbol, digit/XP combo, or brand name and may legitimately be Arabic-free. */
    private val arabicFreeAllowed = setOf(
        "app_name",
        "synapse_screen_dashboard",
        "home_brand_name",
        "provider_chatgpt",
        "provider_deepseek",
        "provider_gemini",
        "provider_groq",
        "provider_huggingface",
        "stat_xp",
        "correct_symbol",
        "incorrect_symbol",
        "percent_mark",
        "percent_value",
        "score_correct_of_total",
        "quiz_question_index",
        "format_label_value",
        "format_dot_value",
        "stat_xp_progress",
        "stat_total_xp_value",
        "stat_xp_to_next",
        "stat_total_xp",
    )

    private fun resourceFile(relative: String): File {
        var dir = File(System.getProperty("user.dir"))
        while (true) {
            val candidate = File(dir, relative)
            if (candidate.isFile) return candidate
            dir = dir.parentFile ?: error("resource not found from user.dir: $relative")
        }
    }

    private fun entries(relative: String): List<Pair<String, String>> {
        val text = resourceFile(relative).readText()
        return stringEntry.findAll(text).map { it.groupValues[1] to it.groupValues[2] }.toList()
    }

    private fun pluralsMap(relative: String): Map<String, Map<String, String>> {
        val text = resourceFile(relative).readText()
        val result = LinkedHashMap<String, Map<String, String>>()
        val blockMatches = pluralBlock.findAll(text)
        for (m in blockMatches) {
            val name = m.groupValues[1]
            val block = m.groupValues[2]
            val items = LinkedHashMap<String, String>()
            val itemMatches = pluralItem.findAll(block)
            for (im in itemMatches) {
                items[im.groupValues[1]] = im.groupValues[2]
            }
            result[name] = items
        }
        return result
    }

    @Test
    fun `P1 same key set and count`() {
        val en = entries(enPath)
        val ar = entries(arPath)
        assertEquals(245, en.size, "English base must hold 245 keys")
        assertEquals(245, ar.size, "Arabic must hold 245 keys")
        assertEquals(
            en.map { it.first }.toSet(),
            ar.map { it.first }.toSet(),
            "key sets must be identical",
        )
    }

    @Test
    fun `P2 identical ordered placeholders per key`() {
        val en = entries(enPath).toMap()
        val ar = entries(arPath).toMap()
        val mismatches = mutableListOf<String>()
        for ((key, enValue) in en) {
            val arValue = ar[key] ?: continue
            val enPlaceholders = placeholder.findAll(enValue).map { it.value }.toList()
            val arPlaceholders = placeholder.findAll(arValue).map { it.value }.toList()
            if (enPlaceholders != arPlaceholders) {
                mismatches += "$key: en=$enPlaceholders ar=$arPlaceholders"
            }
        }
        assertTrue(mismatches.isEmpty(), "placeholder mismatches:\n" + mismatches.joinToString("\n"))
    }

    @Test
    fun `P3 session_complete_message keeps the newline`() {
        val enValue = entries(enPath).toMap().getValue("session_complete_message")
        val arValue = entries(arPath).toMap().getValue("session_complete_message")
        assertTrue(enValue.contains("\\n"), "English session_complete_message must keep \\n")
        assertTrue(arValue.contains("\\n"), "Arabic session_complete_message must keep \\n")
    }

    @Test
    fun `P4 English base carries no Arabic script`() {
        val offenders = entries(enPath).filter { (_, value) -> arabicScript.containsMatchIn(value) }
        assertTrue(
            offenders.isEmpty(),
            "Arabic script found in English base:\n" + offenders.joinToString("\n") { it.first },
        )
    }

    @Test
    fun `P5 Arabic strings are arabic except known symbols and brands`() {
        val offenders = entries(arPath)
            .filter { (key, _) -> key !in arabicFreeAllowed }
            .filter { (_, value) -> !arabicScript.containsMatchIn(value) }
            .map { it.first }
            .toList()
        assertTrue(
            offenders.isEmpty(),
            "Arabic keys without Arabic script (not in the allowed set):\n" + offenders.joinToString("\n"),
        )
    }

    @Test
    fun `P6 plurals key sets identical and exactly the eight`() {
        val expected = setOf("home_streak_value","home_due_label","home_new_label","progress_best_streak","next_review_in_days","next_review_in_weeks","next_review_in_months","session_words_reviewed")
        val enP = pluralsMap(enPluralsPath)
        val arP = pluralsMap(arPluralsPath)
        assertEquals(expected, enP.keys, "English plurals keys must be exactly the eight")
        assertEquals(expected, arP.keys, "Arabic plurals keys must be exactly the eight")
        assertEquals(enP.keys, arP.keys, "plurals key sets must be identical")
    }

    @Test
    fun `P7 plural quantities`() {
        val enP = pluralsMap(enPluralsPath)
        val arP = pluralsMap(arPluralsPath)
        for ((k, items) in enP) {
            assertEquals(setOf("one", "other"), items.keys, "English plural $k must have one and other only")
        }
        val arQty = setOf("zero", "one", "two", "few", "many", "other")
        for ((k, items) in arP) {
            assertEquals(arQty, items.keys, "Arabic plural $k must have zero,one,two,few,many,other")
        }
    }

    @Test
    fun `P8 plurals placeholders only %1$d`() {
        val enP = pluralsMap(enPluralsPath)
        val arP = pluralsMap(arPluralsPath)
        val mismatches = mutableListOf<String>()
        for ((k, items) in enP) {
            for ((q, v) in items) {
                val ph = placeholder.findAll(v).map { it.value }.toList()
                if (ph != listOf("%1\$d")) mismatches += "en $k/$q: $ph"
            }
        }
        for ((k, items) in arP) {
            for ((q, v) in items) {
                val ph = placeholder.findAll(v).map { it.value }.toList()
                if (ph != listOf("%1\$d")) mismatches += "ar $k/$q: $ph"
            }
        }
        assertTrue(mismatches.isEmpty(), "plural placeholder mismatches:\n" + mismatches.joinToString("\n"))
    }

    @Test
    fun `P9 plurals language rules`() {
        val enP = pluralsMap(enPluralsPath)
        val arP = pluralsMap(arPluralsPath)
        val enOff = mutableListOf<String>()
        for ((k, items) in enP) {
            for ((q, v) in items) {
                if (arabicScript.containsMatchIn(v)) enOff += "en $k/$q"
            }
        }
        assertTrue(enOff.isEmpty(), "Arabic script in English plurals:\n" + enOff.joinToString("\n"))
        val arOff = mutableListOf<String>()
        for ((k, items) in arP) {
            for ((q, v) in items) {
                if (!arabicScript.containsMatchIn(v)) arOff += "ar $k/$q"
            }
        }
        assertTrue(arOff.isEmpty(), "Arabic missing in Arabic plurals:\n" + arOff.joinToString("\n"))
    }

    @Test
    fun `P10 no duplicate string keys for migrated plurals`() {
        val migrated = setOf("home_streak_value","home_due_label","home_new_label","progress_best_streak","next_review_in_days","next_review_in_weeks","next_review_in_months","session_words_reviewed","session_words_reviewed_hm","session_words_reviewed_s")
        val enStr = entries(enPath).map { it.first }.toSet()
        val arStr = entries(arPath).map { it.first }.toSet()
        val offenders = mutableSetOf<String>()
        offenders.addAll(enStr.intersect(migrated))
        offenders.addAll(arStr.intersect(migrated))
        assertTrue(offenders.isEmpty(), "Found <string> keys that must become <plurals>:\n" + offenders.joinToString("\n"))
    }
}
