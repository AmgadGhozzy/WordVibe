package com.venom.stackcard.ui.components.mastery

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.venom.wordvibe.R
import com.venom.ui.components.common.adp
import com.venom.designsystem.theme.Emerald400
import com.venom.designsystem.theme.Emerald500
import com.venom.designsystem.theme.Green500
import kotlinx.coroutines.delay

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
fun AnimatedSuccessIcon(
    icon: Int = R.drawable.ic_badge_check,
    modifier: Modifier = Modifier,
    accentColor: Color = Emerald500,
    gradientColors: List<Color> = listOf(
        Emerald500,
        Green500,
        Emerald400
    ),
    outerSize: Int = 130,
    innerSize: Int = 76,
    entryDelay: Long = 200L
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val ringColor = if (isDark) accentColor.copy(alpha = 0.9f) else accentColor

    val entryScale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(entryDelay)
        entryScale.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }

    val transition = rememberInfiniteTransition(label = "successPulse")

    val pulseScale by transition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        )
    )

    val arcRotation by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
    )

    val glowAlpha by transition.animateFloat(
        initialValue = 0.15f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        )
    )

    val midSize = ((outerSize + innerSize) / 2f).toInt()
    val ringLayerSize = outerSize - 15

    Box(modifier = modifier.scale(entryScale.value), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(outerSize.adp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            ringColor.copy(alpha = glowAlpha),
                            ringColor.copy(alpha = glowAlpha * 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )

        val sw = 2.adp
        Box(
            modifier = Modifier
                .size(ringLayerSize.adp)
                .rotate(arcRotation)
                .drawBehind {
                    val sw = sw.toPx()
                    val r = (size.minDimension - sw) / 2
                    val tl = Offset((size.width - r * 2) / 2, (size.height - r * 2) / 2)
                    val s = Size(r * 2, r * 2)
                    drawArc(
                        color = ringColor.copy(0.35f),
                        startAngle = 0f,
                        sweepAngle = 120f,
                        useCenter = false,
                        style = Stroke(width = sw, cap = StrokeCap.Round),
                        topLeft = tl,
                        size = s
                    )
                    drawArc(
                        color = ringColor.copy(0.2f),
                        startAngle = 180f,
                        sweepAngle = 80f,
                        useCenter = false,
                        style = Stroke(width = sw, cap = StrokeCap.Round),
                        topLeft = tl,
                        size = s
                    )
                }
        )

        Box(
            modifier = Modifier
                .size(midSize.adp)
                .clip(CircleShape)
                .background(ringColor.copy(alpha = 0.1f))
                .border(
                    1.5.adp,
                    Brush.sweepGradient(
                        listOf(
                            ringColor.copy(0.3f), ringColor.copy(0.05f),
                            ringColor.copy(0.2f), ringColor.copy(0.05f),
                            ringColor.copy(0.3f)
                        )
                    ),
                    CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(innerSize.adp)
                .clip(CircleShape)
                .background(Brush.linearGradient(gradientColors)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size((innerSize / 2).adp),
                tint = Color.White
            )
        }
    }
}


