package com.venom.wordvibe.ui.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * WordVibe semantic palette. Learning states are the product's core vocabulary on
 * screen, so each KnownState owns a dedicated hue that must stay readable in both
 * themes (contrast-checked pairs below).
 */
@Immutable
data class WordStateColors(
    val isNew: Color,
    val isNewContainer: Color,
    val isLearning: Color,
    val isLearningContainer: Color,
    val isKnown: Color,
    val isKnownContainer: Color,
    val isMastered: Color,
    val isMasteredContainer: Color,
    val isDue: Color,
    val isDueContainer: Color,
)

private val LightWordStates = WordStateColors(
    isNew = Color(0xFF0369A1),
    isNewContainer = Color(0xFFE0F2FE),
    isLearning = Color(0xFF92400E),
    isLearningContainer = Color(0xFFFEF3C7),
    isKnown = Color(0xFF047857),
    isKnownContainer = Color(0xFFD1FAE5),
    isMastered = Color(0xFF6D28D9),
    isMasteredContainer = Color(0xFFEDE9FE),
    isDue = Color(0xFF9A3412),
    isDueContainer = Color(0xFFFFEDD5),
)

private val DarkWordStates = WordStateColors(
    isNew = Color(0xFF7DD3FC),
    isNewContainer = Color(0xFF0C4A6E),
    isLearning = Color(0xFFFCD34D),
    isLearningContainer = Color(0xFF451A03),
    isKnown = Color(0xFF6EE7B7),
    isKnownContainer = Color(0xFF064E3B),
    isMastered = Color(0xFFC4B5FD),
    isMasteredContainer = Color(0xFF2E1065),
    isDue = Color(0xFFFDBA74),
    isDueContainer = Color(0xFF431407),
)

fun wordStateColors(dark: Boolean): WordStateColors =
    if (dark) DarkWordStates else LightWordStates

private val Seed = Color(0xFF4C5FD5)

val WordVibeLightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6F8FB),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFFE2E8F0),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFF1F5F9),
)

val WordVibeDarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF091936),
    primaryContainer = Color(0xFF172554),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF34D399),
    onSecondary = Color(0xFF022C22),
    secondaryContainer = Color(0xFF263244),
    onSecondaryContainer = Color(0xFFF1F5F9),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF451A03),
    onTertiaryContainer = Color(0xFFFEF3C7),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0B111E),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF0B111E),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF263244),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF263244),
    surfaceContainerLow = Color(0xFF151E2E),
    surfaceContainer = Color(0xFF151E2E),
    surfaceContainerHigh = Color(0xFF151E2E),
    surfaceContainerHighest = Color(0xFF1E293B),
)
