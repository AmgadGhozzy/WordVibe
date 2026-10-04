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
    isNew = Color(0xFF35618E),
    isNewContainer = Color(0xFFD6E3F6),
    isLearning = Color(0xFF8A5300),
    isLearningContainer = Color(0xFFFFDCB9),
    isKnown = Color(0xFF206C4C),
    isKnownContainer = Color(0xFFB7F1CF),
    isMastered = Color(0xFF8E4957),
    isMasteredContainer = Color(0xFFFFD9DE),
    isDue = Color(0xFF954000),
    isDueContainer = Color(0xFFFFDBBE),
)

private val DarkWordStates = WordStateColors(
    isNew = Color(0xFFAAC7F0),
    isNewContainer = Color(0xFF1D4468),
    isLearning = Color(0xFFFFBA6C),
    isLearningContainer = Color(0xFF5B3F00),
    isKnown = Color(0xFF8CD5AE),
    isKnownContainer = Color(0xFF085333),
    isMastered = Color(0xFFFFB1BE),
    isMasteredContainer = Color(0xFF6C3341),
    isDue = Color(0xFFFFB77E),
    isDueContainer = Color(0xFF6E3200),
)

fun wordStateColors(dark: Boolean): WordStateColors =
    if (dark) DarkWordStates else LightWordStates

private val Seed = Color(0xFF4C5FD5)

val WordVibeLightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF4C5FD5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDEE0FF),
    onPrimaryContainer = Color(0xFF00105C),
    secondary = Color(0xFF206C4C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB7F1CF),
    onSecondaryContainer = Color(0xFF072115),
    tertiary = Color(0xFF8E4957),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9DE),
    onTertiaryContainer = Color(0xFF3B0716),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF46464F),
    outline = Color(0xFF767680),
    outlineVariant = Color(0xFFC7C5D0),
)

val WordVibeDarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFBAC3FF),
    onPrimary = Color(0xFF182778),
    primaryContainer = Color(0xFF314190),
    onPrimaryContainer = Color(0xFFDEE0FF),
    secondary = Color(0xFF8CD5AE),
    onSecondary = Color(0xFF085333),
    secondaryContainer = Color(0xFF085333),
    onSecondaryContainer = Color(0xFFB7F1CF),
    tertiary = Color(0xFFFFB1BE),
    onTertiary = Color(0xFF541D29),
    tertiaryContainer = Color(0xFF6C3341),
    onTertiaryContainer = Color(0xFFFFD9DE),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF131318),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF131318),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
    outline = Color(0xFF90909A),
    outlineVariant = Color(0xFF46464F),
)
