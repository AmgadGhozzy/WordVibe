package com.venom.wordvibe.ui.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * WordVibe product theme: custom Material 3, light/dark, Arabic-first.
 * RTL is the default orientation for product chrome; screens that show English
 * learning content opt into LTR locally (never the reverse).
 */
val LocalWordStateColors = staticCompositionLocalOf { wordStateColors(dark = false) }

@Composable
fun WordVibeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) WordVibeDarkColors else WordVibeLightColors

    CompositionLocalProvider(
        LocalWordStateColors provides wordStateColors(darkTheme),
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = WordVibeTypography,
            shapes = WordVibeShapes,
            content = content,
        )
    }
}

/** Arabic text flows RTL regardless of any local LTR block (e.g. English word rows). */
val CurrentDirection: LayoutDirection
    @Composable get() = LocalLayoutDirection.current
