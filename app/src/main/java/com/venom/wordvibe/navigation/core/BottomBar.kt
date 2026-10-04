package com.venom.wordvibe.navigation.core

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.venom.designsystem.theme.SynapseTheme
import com.venom.wordvibe.navigation.SynapseNavigationItems

private data class BarColors(
    val glassTop: Color,
    val glassBottom: Color,
    val borderHighlight: Color,
    val borderShadow: Color,
    val shimmerA: Color,
    val shimmerB: Color,
    val shimmerC: Color,
    val glow: Color,
    val iconSelected: Color,
    val iconUnselected: Color,
    val arcStroke: Color,
)

@Composable
private fun rememberBarColors(isDark: Boolean): BarColors {
    val cs = MaterialTheme.colorScheme
    val primary = cs.primary

    return remember(isDark, primary) {
        if (isDark) BarColors(
            glassTop = cs.surface.copy(alpha = 0.92f),
            glassBottom = cs.background.copy(alpha = 0.88f),
            borderHighlight = primary.copy(alpha = 0.18f),
            borderShadow = primary.copy(alpha = 0.06f),
            shimmerA = Color.White.copy(alpha = 0.03f),
            shimmerB = primary.copy(alpha = 0.06f),
            shimmerC = Color.White.copy(alpha = 0.02f),
            glow = primary.copy(alpha = 0.35f),
            iconSelected = primary,
            iconUnselected = cs.onSurfaceVariant.copy(alpha = 0.6f),
            arcStroke = primary.copy(alpha = 0.90f),
        ) else BarColors(
            glassTop = cs.surface.copy(alpha = 0.96f),
            glassBottom = cs.background.copy(alpha = 0.90f),
            borderHighlight = primary.copy(alpha = 0.22f),
            borderShadow = primary.copy(alpha = 0.08f),
            shimmerA = Color.White.copy(alpha = 0.55f),
            shimmerB = primary.copy(alpha = 0.08f),
            shimmerC = Color.White.copy(alpha = 0.30f),
            glow = primary.copy(alpha = 0.15f),
            iconSelected = primary,
            iconUnselected = cs.onSurfaceVariant.copy(alpha = 0.6f),
            arcStroke = primary.copy(alpha = 0.80f),
        )
    }
}

@Composable
fun BottomBar(
    items: List<NavigationItem>,
    navController: NavHostController,
    currentRoute: String?,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colors = rememberBarColors(isDark)
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    val visibleItems = remember(items) { items.filter { it.showInBottomBar } }

    val selectedIndex by remember(currentRoute, visibleItems) {
        derivedStateOf {
            visibleItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
        }
    }

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            stiffness = Spring.StiffnessVeryLow,
            dampingRatio = Spring.DampingRatioLowBouncy,
        )
    )

    val animatedGlow by animateColorAsState(
        targetValue = colors.glow,
        animationSpec = spring(stiffness = Spring.StiffnessVeryLow)
    )
    val cornerRadius = 50.dp
    val barShape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .wrapContentWidth()
            .width(IntrinsicSize.Min)
            .scale(0.95f)
            .offset(y = (-12).dp)
            .drawWithCache {
                val brush = Brush.verticalGradient(listOf(colors.glassTop, colors.glassBottom))
                onDrawBehind {
                    drawRoundRect(brush = brush, cornerRadius = CornerRadius(cornerRadius.toPx()))
                }
            }
            .border(
                width = Dp.Hairline,
                brush = Brush.verticalGradient(listOf(colors.borderHighlight, colors.borderShadow)),
                shape = barShape,
            )
            .clip(barShape),
    ) {

        // ── Glass shimmer fill ────────────────────────────────────────────────
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(colors.shimmerA, colors.shimmerB, colors.shimmerC)
                ),
                cornerRadius = CornerRadius(cornerRadius.toPx()),
            )
        }

        // ── Glow blob  ─────────────────────────────────────────────────────────
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .clip(barShape)
        ) {
            withTransform({ if (isRtl) scale(scaleX = -1f, scaleY = 1f, pivot = center) }) {
                val tabWidth = size.width / visibleItems.size

                val centerOffset = Offset(
                    x = tabWidth * animatedIndex + tabWidth / 2f,
                    y = size.height / 2f,
                )
                val glowColor = animatedGlow.copy(alpha = if (isDark) 0.55f else 0.35f)
                val glowRadius = size.height * 1.5f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent),
                        center = centerOffset,
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = centerOffset,
                )
            }
        }

        // ── Arc stroke on active tab ──────────────────────────────────────────
        Canvas(modifier = Modifier
            .matchParentSize()
            .clip(barShape)) {
            withTransform({ if (isRtl) scale(scaleX = -1f, scaleY = 1f, pivot = center) }) {
                val tabWidth = size.width / visibleItems.size
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            size.toRect(),
                            CornerRadius(size.height)
                        )
                    )
                }
                val pathLen = PathMeasure().apply { setPath(path, false) }.length

                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            colors.arcStroke.copy(alpha = 0.00f),
                            colors.arcStroke.copy(alpha = 0.90f),
                            colors.arcStroke.copy(alpha = 0.90f),
                            colors.arcStroke.copy(alpha = 0.00f),
                        ),
                        startX = tabWidth * animatedIndex,
                        endX = tabWidth * (animatedIndex + 1f),
                    ),
                    style = Stroke(
                        width = 3.5f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(pathLen / 2f, pathLen),
                        ),
                    ),
                )
            }
        }

        // ── Tab items ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .padding(top = 12.dp, bottom = 8.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            visibleItems.forEach { item ->
                val isSelected by remember(currentRoute, item) {
                    derivedStateOf { currentRoute == item.route }
                }

                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.25f else 1f,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessVeryLow,
                        dampingRatio = Spring.DampingRatioLowBouncy,
                    ),
                    visibilityThreshold = 0.000001f
                )

                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) colors.iconSelected else colors.iconUnselected,
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                )
                val labelColor by animateColorAsState(
                    targetValue = if (isSelected) colors.iconSelected else colors.iconUnselected,
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                )
                val labelWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(item) {
                            detectTapGestures { navController.navigateToStart(item.route) }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (isSelected) item.iconActive else item.icon
                            ),
                            contentDescription = stringResource(item.titleRes),
                            tint = iconTint,
                            modifier = Modifier
                                .size(30.dp)
                                .scale(iconScale),
                        )

                        Text(
                            text = stringResource(item.titleRes),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = labelWeight,
                            color = labelColor,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Android 12 (API 31)", apiLevel = 31, showBackground = true)
@Preview(name = "Android 8 (API 26)", apiLevel = 36, showBackground = true)
@Composable
private fun BottomBarPreview() {
    SynapseTheme {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            BottomBar(
                items = SynapseNavigationItems.visibleItems,
                navController = rememberNavController(),
                currentRoute = SynapseNavigationItems.DASHBOARD.route
            )
        }
    }
}