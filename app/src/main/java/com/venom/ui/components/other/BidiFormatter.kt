package com.venom.ui.components.other

import androidx.core.text.BidiFormatter
import androidx.core.text.TextDirectionHeuristicsCompat

object BiDiFormatter {

    private val rtlFormatter = BidiFormatter.getInstance(true)
    private val ltrFormatter = BidiFormatter.getInstance(false)

    fun format(text: String): String {
        if (text.isBlank()) return text

        val containsArabic =
            text.any { Character.UnicodeBlock.of(it)?.toString()?.contains("ARABIC") == true }

        return if (containsArabic) {
            rtlFormatter.unicodeWrap(
                text,
                TextDirectionHeuristicsCompat.RTL
            )
        } else {
            ltrFormatter.unicodeWrap(
                text,
                TextDirectionHeuristicsCompat.LTR
            )
        }
    }
}