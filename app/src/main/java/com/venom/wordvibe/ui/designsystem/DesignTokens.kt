package com.venom.wordvibe.ui.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * WordVibe design tokens: spacing scale, corner radii, and touch-target floor.
 * Values are RTL-neutral (start/end-safe) and tuned for Arabic-first density.
 */
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 48.dp
}

object Radii {
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val pill: Dp = 999.dp
}

object Elevations {
    val card = 1.dp
    val raised = 3.dp
    val overlay = 8.dp
}

/** Minimum interactive target per WCAG/M3 — every clickable element honours this. */
val MinTouchTarget = 48.dp

val WordVibeShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(Radii.sm),
    small = RoundedCornerShape(Radii.sm),
    medium = RoundedCornerShape(Radii.md),
    large = RoundedCornerShape(Radii.lg),
    extraLarge = RoundedCornerShape(Radii.lg),
)
