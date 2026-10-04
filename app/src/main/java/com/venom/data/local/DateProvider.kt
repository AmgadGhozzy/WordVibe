package com.venom.data.local

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Day-key arithmetic behind streaks, activity days, and event dateKeys.
 * Injectable so tests run on a fixed calendar; the system implementation creates
 * a new formatter per call because SimpleDateFormat is not thread-safe and the
 * repositories serve concurrent IO coroutines.
 */
interface DateProvider {
    fun now(): Long
    fun todayKey(): String
    fun yesterdayKey(): String
    fun previousDayKey(dateKey: String): String
}

class SystemDateProvider : DateProvider {

    private fun format(date: Date): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)

    override fun now(): Long = System.currentTimeMillis()

    override fun todayKey(): String = format(Date(now()))

    override fun yesterdayKey(): String = format(Date(now() - DAY_MS))

    override fun previousDayKey(dateKey: String): String = try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        format(Date(parser.parse(dateKey).time - DAY_MS))
    } catch (_: Exception) {
        dateKey
    }

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
