package com.venom.wordvibe.core.theme.tokens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
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
fun getCefrColorScheme(level: CefrLevel): CefrColorScheme = when (level) {
    CefrLevel.A1 -> CefrColorScheme(
        background = Emerald50.copy(alpha = 0.8f),
        border = Emerald600.copy(alpha = 0.3f),
        content = Emerald400,
    )
    CefrLevel.A2 -> CefrColorScheme(
        background = Teal50.copy(alpha = 0.8f),
        border = Teal600.copy(alpha = 0.3f),
        content = Teal400,
    )
    CefrLevel.B1 -> CefrColorScheme(
        background = Blue50.copy(alpha = 0.8f),
        border = Blue600.copy(alpha = 0.3f),
        content = Blue400,
    )
    CefrLevel.B2 -> CefrColorScheme(
        background = Indigo50.copy(alpha = 0.8f),
        border = Indigo600.copy(alpha = 0.3f),
        content = Indigo400,
    )
    CefrLevel.C1 -> CefrColorScheme(
        background = Purple50.copy(alpha = 0.8f),
        border = Purple600.copy(alpha = 0.3f),
        content = Purple400,
    )
    CefrLevel.C2 -> CefrColorScheme(
        background = Amber50.copy(alpha = 0.8f),
        border = Amber600.copy(alpha = 0.3f),
        content = Amber400,
    )
}

// ── DifficultyTheme ──────────────────────────────────────────────────────────

@Immutable
data class DifficultyTheme(
    val background: Color,
    val border: Color,
    val text: Color,
)

@Composable
fun getDifficultyTheme(score: Int): DifficultyTheme = when {
    score <= 2 -> DifficultyTheme(
        background = Green50.copy(alpha = 0.8f),
        border = Green600.copy(alpha = 0.3f),
        text = Green400,
    )
    score <= 4 -> DifficultyTheme(
        background = Teal50.copy(alpha = 0.8f),
        border = Teal600.copy(alpha = 0.3f),
        text = Teal400,
    )
    score <= 6 -> DifficultyTheme(
        background = Blue50.copy(alpha = 0.8f),
        border = Blue600.copy(alpha = 0.3f),
        text = Blue400,
    )
    score <= 8 -> DifficultyTheme(
        background = Amber50.copy(alpha = 0.8f),
        border = Amber600.copy(alpha = 0.3f),
        text = Amber400,
    )
    else -> DifficultyTheme(
        background = Red50.copy(alpha = 0.8f),
        border = Red600.copy(alpha = 0.3f),
        text = Red400,
    )
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
