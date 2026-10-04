package com.venom.ui.components.other

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.venom.ui.components.common.adp
import com.venom.designsystem.theme.synapse

enum class GlassThickness { UltraThin, Thin, Regular, Thick, UltraThick }
enum class GlassTint { Primary, Secondary, Accent }

@Composable
private fun GlassThickness.toElevation(): Dp = when (this) {
    GlassThickness.UltraThin -> 2.adp
    GlassThickness.Thin -> 4.adp
    GlassThickness.Regular -> 6.adp
    GlassThickness.Thick -> 8.adp
    GlassThickness.UltraThick -> 12.adp
}

@Composable
private fun GlassThickness.toLayer(): Color {
    return MaterialTheme.colorScheme.surfaceContainerHigh
}

@Composable
private fun GlassThickness.toBorderColor(): Color {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return when (this) {
        GlassThickness.UltraThin, GlassThickness.Thin -> Color.White.copy(alpha = if (isDark) 0.10f else 0.20f)
        else -> Color.White.copy(alpha = if (isDark) 0.20f else 0.40f)
    }
}

/**
 * A premium glassmorphism card with Apple-inspired liquid aesthetics.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    thickness: GlassThickness = GlassThickness.Regular,
    tint: GlassTint? = GlassTint.Primary,
    showBorder: Boolean = false,
    showShadow: Boolean = true,
    shape: Shape = RoundedCornerShape(20.adp),
    contentPadding: Dp = 0.adp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val glassLayer = thickness.toLayer()
    val borderColor = thickness.toBorderColor()
    val elevation = thickness.toElevation()

    val tintColor: Color = when (tint) {
        GlassTint.Primary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        GlassTint.Secondary -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
        GlassTint.Accent -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
        null -> Color.Transparent
    }

    Box(
        modifier = modifier
            .then(
                if (showShadow) Modifier.shadow(
                    elevation = elevation,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.2f),
                    spotColor = Color.Black.copy(alpha = 0.3f),
                ) else Modifier,
            )
            .clip(shape)
            .background(glassLayer)
            .then(if (tintColor != Color.Transparent) Modifier.background(tintColor) else Modifier)
            .then(if (showBorder) Modifier.border(1.adp, borderColor, shape) else Modifier)
            .then(
                if (onClick != null) Modifier.clickable(
                    onClick = onClick,
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = MaterialTheme.colorScheme.primary),
                ) else Modifier,
            ),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.adp),
            content = content,
        )
    }
}

/**
 * Premium gradient glass card inspired by Synapse's CardShell.
 *
 * Features:
 * - Directional shadow with proper ambient/spot tones
 * - Gradient border (subtle white alpha fade)
 * - Radial glow ("orbet") in top-right corner for depth
 * - Gradient tint overlay on top of the glass fill
 */
@Composable
fun GradientGlassCard(
    modifier: Modifier = Modifier,
    thickness: GlassThickness = GlassThickness.Regular,
    gradientColors: List<Color> = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
    gradientAlpha: Float = 0.08f,
    showOrbet: Boolean = true,
    orbetColor: Color = MaterialTheme.colorScheme.primary,
    showBorder: Boolean = true,
    showShadow: Boolean = true,
    shadowColor: Color = Color.Unspecified,
    shape: Shape = RoundedCornerShape(24.adp),
    borderColor: Color = Color.White.copy(alpha = 0.15f),
    contentPadding: Dp = 0.adp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassLayer = thickness.toLayer()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (showShadow) Modifier.shadow(
                    elevation = 16.adp,
                    shape = shape,
                    ambientColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.10f),
                    spotColor = if (isDark) Color.Black.copy(alpha = 0.50f) else Color.Black.copy(alpha = 0.15f),
                ) else Modifier,
            )
            .clip(shape)
            .background(glassLayer)
            .background(
                brush = if (gradientColors.size >= 2) {
                    Brush.linearGradient(
                        colors = gradientColors.map { it.copy(alpha = it.alpha * gradientAlpha) },
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                    )
                } else {
                    SolidColor(
                        gradientColors.firstOrNull()
                            ?.let { it.copy(alpha = it.alpha * gradientAlpha) }
                            ?: Color.Transparent
                    )
                },
            )
            .then(
                if (showBorder) {
                    Modifier.border(
                        width = 1.adp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.15f else 0.40f),
                                Color.White.copy(alpha = if (isDark) 0.05f else 0.15f),
                            )
                        ),
                        shape = shape,
                    )
                } else Modifier
            )
            .then(
                if (onClick != null) Modifier.clickable(
                    onClick = onClick,
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = MaterialTheme.colorScheme.primary),
                ) else Modifier,
            ),
    ) {
        if (showOrbet) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 40.adp, y = (-48).adp)
                    .size(180.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                orbetColor.copy(alpha = if (isDark) 0.10f else 0.15f),
                                Color.Transparent,
                            ),
                            radius = 220f,
                        ),
                        shape = CircleShape,
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content,
        )
    }
}

private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}
