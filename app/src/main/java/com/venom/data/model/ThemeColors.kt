package com.venom.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ThemeColor(
    val name: String,
    val color: Long
)

fun generateThemeColors() = listOf(
    ThemeColor("Sky Blue", 0xFF00C853),
    ThemeColor("Soft Pink", 0xFFFFC6E5),
    ThemeColor("Dusty Rose", 0xFFE67AB2),
    ThemeColor("Orchid", 0xFFD186E0),
    ThemeColor("Periwinkle", 0xFFB088FF),
    ThemeColor("Cornflower", 0xFF6B8CFF),
    ThemeColor("Royal Blue", 0xFF5E82F0),
    ThemeColor("Ocean Blue", 0xFF0099EE),
    ThemeColor("Turquoise", 0xFF1EEBB8),
    ThemeColor("Jade", 0xFF00CC66),
)
