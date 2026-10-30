package com.venom.wordvibe.ui.viewmodel

import java.util.Calendar

/* ───── Progress analytics time-series bucketing (pure, unit-testable) ───── */

/**
 * Local-wall-clock dateKeys (yyyy-MM-dd) for the N days ending today, oldest
 * first. Day boundaries are walked one calendar day at a time (b6b114d forecast
 * precedent), so DST shifts keep one key per local day.
 */
internal fun dateKeysForWindow(now: Long, days: Int): List<String> {
    require(days > 0) { "window must be positive" }
    val cal = Calendar.getInstance()
    cal.timeInMillis = now
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val keys = ArrayList<String>(days)
    repeat(days) {
        keys.add(
            String.format(
                "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH),
            )
        )
        cal.add(Calendar.DAY_OF_YEAR, -1)
    }
    keys.reverse()
    return keys
}

/**
 * Compact series oldest→newest: measured counts mapped onto the window keys,
 * zero-filled for every quiet day. Offline result rows outside the window are
 * ignored (the window keys are the only axis).
 */
internal fun buildSeries(dateKeys: List<String>, countsByDate: Map<String, Int>): List<Int> =
    dateKeys.map { countsByDate[it] ?: 0 }
