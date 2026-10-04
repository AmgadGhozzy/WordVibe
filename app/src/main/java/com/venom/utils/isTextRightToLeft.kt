package com.venom.utils

import androidx.compose.ui.text.style.TextDirection

// Determine if the text direction is RTL or LTR
fun getTextDirection(text: String): TextDirection {
    if (text.isEmpty()) return TextDirection.Ltr
    val rtlChars = "\u0591-\u07FF\uFB1D-\uFDFD\uFE70-\uFEFC"
    val rtlRegex = Regex("[$rtlChars]")
    return if (rtlRegex.containsMatchIn(text)) TextDirection.Rtl else TextDirection.Ltr
}

// Check if text is right-to-left
fun isTextRightToLeft(text: String): Boolean {
    if (text.isEmpty()) return false
    val rtlChars = "\u0591-\u07FF\uFB1D-\uFDFD\uFE70-\uFEFC"
    val rtlRegex = Regex("[$rtlChars]")
    return rtlRegex.containsMatchIn(text)
}
