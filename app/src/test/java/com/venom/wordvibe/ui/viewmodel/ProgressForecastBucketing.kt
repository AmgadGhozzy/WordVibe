package com.venom.wordvibe.ui.viewmodel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Calendar

class ProgressForecastBucketingTest {

    /** Local wall-clock time at [hour]:00 on today + [dayOffset] days. */
    private fun localTime(now: Long, hour: Int, dayOffset: Long = 0): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, dayOffset.toInt())
        cal.set(Calendar.HOUR_OF_DAY, hour)
        return cal.timeInMillis
    }

    @Test
    fun `same-day times aggregate into today's bucket`() {
        val now = localTime(System.currentTimeMillis(), 10)
        val times = listOf(
            localTime(now, 11),
            localTime(now, 12),
            localTime(now, 22),
        )
        val forecast = buildSevenDayForecast(times, now)
        assertEquals(listOf(3, 0, 0, 0, 0, 0, 0), forecast)
    }

    @Test
    fun `time exactly at next local midnight lands in tomorrow bucket`() {
        val now = localTime(System.currentTimeMillis(), 10)
        val times = listOf(localTime(now, 0, dayOffset = 1))
        val forecast = buildSevenDayForecast(times, now)
        assertEquals(listOf(0, 1, 0, 0, 0, 0, 0), forecast)
    }

    @Test
    fun `empty schedule yields seven zeros`() {
        assertEquals(
            listOf(0, 0, 0, 0, 0, 0, 0),
            buildSevenDayForecast(emptyList(), System.currentTimeMillis()),
        )
    }

    @Test
    fun `far future time beyond six days is ignored`() {
        val now = localTime(System.currentTimeMillis(), 10)
        val times = listOf(localTime(now, 9, dayOffset = 7), localTime(now, 9, dayOffset = 30))
        val forecast = buildSevenDayForecast(times, now)
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 0), forecast)
    }

    @Test
    fun `spread across seven days counts one per day`() {
        val now = localTime(System.currentTimeMillis(), 10)
        val times = (0..6).map { localTime(now, 9, dayOffset = it.toLong()) }
        val forecast = buildSevenDayForecast(times, now)
        assertEquals(listOf(1, 1, 1, 1, 1, 1, 1), forecast)
    }

    @Test
    fun `past time before today is ignored`() {
        val now = localTime(System.currentTimeMillis(), 10)
        val times = listOf(localTime(now, 9, dayOffset = -1), localTime(now, 15))
        val forecast = buildSevenDayForecast(times, now)
        assertEquals(listOf(1, 0, 0, 0, 0, 0, 0), forecast)
    }
}
