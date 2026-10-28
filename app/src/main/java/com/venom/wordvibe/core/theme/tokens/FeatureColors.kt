package com.venom.wordvibe.core.theme.tokens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.venom.domain.model.CefrLevel
// ─────────────────────────────────────────────────────────────────────────────
// PHASED MIGRATION (F-6):
// FeatureColors uses raw palette colors (Emerald50, Teal400, etc.) from the
// external 'designsystem' module (AAR). The app shell uses WordVibeColors.
// In this phased state, WordVibeTheme/WordVibeColors owns semantic app colors
// (backgrounds, surfaces), while these external tokens are permitted ONLY for
// CEFR levels and Difficulty visual tags.
// ─────────────────────────────────────────────────────────────────────────────
import com.venom.designsystem.theme.*

// ── CefrColorScheme ──────────────────────────────────────────────────────────

@Immutable
data class CefrColorScheme(
    val background: Color,
    val border: Color,
    val content: Color,
)

@Composable
fun getCefrColorScheme(level: CefrLevel): CefrColorScheme {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (level) {
        CefrLevel.A1 -> CefrColorScheme(
            background = if (isDark) Emerald600.copy(alpha = 0.18f) else Emerald50,
            border = if (isDark) Emerald400.copy(alpha = 0.35f) else Emerald600.copy(alpha = 0.25f),
            content = if (isDark) Emerald400 else Emerald600,
        )
        CefrLevel.A2 -> CefrColorScheme(
            background = if (isDark) Teal600.copy(alpha = 0.18f) else Teal50,
            border = if (isDark) Teal400.copy(alpha = 0.35f) else Teal600.copy(alpha = 0.25f),
            content = if (isDark) Teal400 else Teal600,
        )
        CefrLevel.B1 -> CefrColorScheme(
            background = if (isDark) Blue600.copy(alpha = 0.18f) else Blue50,
            border = if (isDark) Blue400.copy(alpha = 0.35f) else Blue600.copy(alpha = 0.25f),
            content = if (isDark) Blue400 else Blue600,
        )
        CefrLevel.B2 -> CefrColorScheme(
            background = if (isDark) Indigo600.copy(alpha = 0.18f) else Indigo50,
            border = if (isDark) Indigo400.copy(alpha = 0.35f) else Indigo600.copy(alpha = 0.25f),
            content = if (isDark) Indigo400 else Indigo600,
        )
        CefrLevel.C1 -> CefrColorScheme(
            background = if (isDark) Purple600.copy(alpha = 0.18f) else Purple50,
            border = if (isDark) Purple400.copy(alpha = 0.35f) else Purple600.copy(alpha = 0.25f),
            content = if (isDark) Purple400 else Purple600,
        )
        CefrLevel.C2 -> CefrColorScheme(
            background = if (isDark) Amber600.copy(alpha = 0.18f) else Amber50,
            border = if (isDark) Amber400.copy(alpha = 0.35f) else Amber600.copy(alpha = 0.25f),
            content = if (isDark) Amber400 else Amber600,
        )
    }
}

// ── DifficultyTheme ──────────────────────────────────────────────────────────

@Immutable
data class DifficultyTheme(
    val background: Color,
    val border: Color,
    val text: Color,
)

@Composable
fun getDifficultyTheme(score: Int): DifficultyTheme {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when {
        score <= 2 -> DifficultyTheme(
            background = if (isDark) Green600.copy(alpha = 0.18f) else Green50,
            border = if (isDark) Green400.copy(alpha = 0.35f) else Green600.copy(alpha = 0.25f),
            text = if (isDark) Green400 else Green600,
        )
        score <= 4 -> DifficultyTheme(
            background = if (isDark) Teal600.copy(alpha = 0.18f) else Teal50,
            border = if (isDark) Teal400.copy(alpha = 0.35f) else Teal600.copy(alpha = 0.25f),
            text = if (isDark) Teal400 else Teal600,
        )
        score <= 6 -> DifficultyTheme(
            background = if (isDark) Blue600.copy(alpha = 0.18f) else Blue50,
            border = if (isDark) Blue400.copy(alpha = 0.35f) else Blue600.copy(alpha = 0.25f),
            text = if (isDark) Blue400 else Blue600,
        )
        score <= 8 -> DifficultyTheme(
            background = if (isDark) Amber600.copy(alpha = 0.18f) else Amber50,
            border = if (isDark) Amber400.copy(alpha = 0.35f) else Amber600.copy(alpha = 0.25f),
            text = if (isDark) Amber400 else Amber600,
        )
        else -> DifficultyTheme(
            background = if (isDark) Red600.copy(alpha = 0.18f) else Red50,
            border = if (isDark) Red400.copy(alpha = 0.35f) else Red600.copy(alpha = 0.25f),
            text = if (isDark) Red400 else Red600,
        )
    }
}

// ── getPosColor ──────────────────────────────────────────────────────────────

@Composable
fun getPosColor(pos: String): Color = when (pos.lowercase()) {
    "noun" -> Blue500
    "verb" -> Red500
    "adjective", "adj" -> Amber500
    "adverb", "adv" -> Purple500
    "pronoun", "pron" -> Teal500
    "preposition", "prep" -> Slate500
    "conjunction", "conj" -> Indigo500
    "interjection", "interj" -> Pink500
    else -> Slate400
}
