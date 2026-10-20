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

    private val stringEntry = Regex("""<string name="([^"]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
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

    @Test
    fun `P1 same key set and count`() {
        val en = entries(enPath)
        val ar = entries(arPath)
        assertEquals(253, en.size, "English base must hold 253 keys")
        assertEquals(253, ar.size, "Arabic must hold 253 keys")
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
}
