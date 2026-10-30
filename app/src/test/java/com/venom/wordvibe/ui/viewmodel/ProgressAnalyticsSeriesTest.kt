package com.venom.wordvibe.ui.viewmodel

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Calendar
import java.util.TimeZone

class ProgressAnalyticsSeriesTest {

    private val originalZone = TimeZone.getDefault()

    @BeforeEach
    fun pinZone() {
        // A DST zone so Calendar-based day walks (b6b114d precedent) are exercised.
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
    }

    @AfterEach
    fun restoreZone() {
        TimeZone.setDefault(originalZone)
    }

    private fun epochAt(year: Int, month: Int, day: Int, hour: Int): Long {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(year, month - 1, day, hour, 0, 0)
        return cal.timeInMillis
    }

    @Test
    fun `dateKeysForWindow returns oldest to newest with today last`() {
        val now = epochAt(2024, 1, 15, 15) // Jan 15 2024 15:00 NY
        val keys = dateKeysForWindow(now, 7)
        assertEquals(7, keys.size)
        assertEquals("2024-01-09", keys.first())
        assertEquals("2024-01-15", keys.last())
    }

    @Test
    fun `dateKeysForWindow is distinct across a DST fall-back day`() {
        // 2024-11-03 repeats 01:00–02:00 (fall-back); midnight simplification must
        // not produce a duplicate key for the repeated local day.
        val now = epochAt(2024, 11, 3, 15)
        val keys = dateKeysForWindow(now, 7)
        assertEquals(7, keys.toSet().size)
        assertEquals("2024-10-28", keys.first())
        assertEquals("2024-11-03", keys.last())
    }

    @Test
    fun `dateKeysForWindow is distinct across a DST spring-forward day`() {
        val now = epochAt(2024, 3, 10, 15)
        val keys = dateKeysForWindow(now, 7)
        assertEquals(7, keys.toSet().size)
        assertEquals("2024-03-04", keys.first())
        assertEquals("2024-03-10", keys.last())
    }

    @Test
    fun `keys are uniformly formatted yyyy-MM-dd`() {
        val now = epochAt(2024, 1, 5, 12)
        dateKeysForWindow(now, 30).forEach { key ->
            assertTrue(Regex("""^\d{4}-\d{2}-\d{2}$""").matches(key), key)
        }
    }

    @Test
    fun `empty result map yields all-zero series`() {
        val keys = dateKeysForWindow(epochAt(2024, 1, 15, 10), 7)
        assertEquals(List(7) { 0 }, buildSeries(keys, emptyMap()))
    }

    @Test
    fun `buildSeries zero-fills empty days and keeps measured days in place`() {
        val keys = dateKeysForWindow(epochAt(2024, 1, 15, 10), 7)
        // Measured days: 2024-01-10 (index 1) and 2024-01-15 (last index).
        val series = buildSeries(keys, mapOf("2024-01-10" to 4, "2024-01-15" to 9))
        assertEquals(listOf(0, 4, 0, 0, 0, 0, 9), series)
    }

    @Test
    fun `unknown dateKeys in the result map are ignored`() {
        val keys = dateKeysForWindow(epochAt(2024, 1, 15, 10), 7)
        val series = buildSeries(keys, mapOf("2023-12-31" to 5, "2024-01-14" to 2))
        assertEquals(listOf(0, 0, 0, 0, 0, 2, 0), series)
    }

    @Test
    fun `30-day window yields thirty keys`() {
        val now = epochAt(2024, 1, 31, 10)
        val keys = dateKeysForWindow(now, 30)
        assertEquals(30, keys.size)
        assertEquals("2024-01-02", keys.first())
        assertEquals("2024-01-31", keys.last())
    }
}
