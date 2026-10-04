package com.venom.ui.components.other

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.venom.wordvibe.R
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp

@Composable
fun CircularProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String? = null,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color? = null,
    fontSize: TextUnit = 10.asp,
    strokeWidthDp: Dp = 8.adp,
    glowEnabled: Boolean = true,
) {
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val resolvedTrackColor = trackColor
        ?: if (isDarkTheme) Color.White.copy(alpha = 0.07f)
        else Color.Black.copy(alpha = 0.08f)

    val animatedProgress by animateFloatAsState(
        targetValue  = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1_200, easing = FastOutSlowInEasing),
    )

    val percentMark = stringResource(R.string.percent_mark)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.drawWithCache {
            val strokePx   = strokeWidthDp.toPx()
            val diameter   = size.minDimension - strokePx
            val topLeft    = Offset(
                x = (size.width  - diameter) / 2f,
                y = (size.height - diameter) / 2f,
            )
            val arcSize    = Size(diameter, diameter)
            val sweepAngle = 360f * animatedProgress

            val glowPaint = if (glowEnabled) Paint().also { p ->
                p.asFrameworkPaint().apply {
                    isAntiAlias  = true
                    style        = android.graphics.Paint.Style.STROKE
                    strokeWidth  = strokePx
                    strokeCap    = android.graphics.Paint.Cap.ROUND
                    color        = android.graphics.Color.TRANSPARENT
                    setShadowLayer(
                        strokePx * 1.8f,           // blur radius — 1.8× stroke
                        0f, 0f,
                        progressColor.copy(alpha = 0.3f).toArgb(),
                    )
                }
            } else null

            onDrawBehind {
                // ── Track ───────────────────────────────────────────────────
                drawArc(
                    color      = resolvedTrackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter  = false,
                    style      = Stroke(width = strokePx, cap = StrokeCap.Round),
                    topLeft    = topLeft,
                    size       = arcSize,
                )

                if (sweepAngle > 0f) {
                    // ── Glow layer (drawn first so arc renders on top) ───────
                    if (glowPaint != null) {
                        drawIntoCanvas { canvas ->
                            canvas.drawArc(
                                left       = topLeft.x,
                                top        = topLeft.y,
                                right      = topLeft.x + arcSize.width,
                                bottom     = topLeft.y + arcSize.height,
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter  = false,
                                paint      = glowPaint,
                            )
                        }
                    }

                    // ── Progress arc ────────────────────────────────────────
                    drawArc(
                        color      = progressColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter  = false,
                        style      = Stroke(width = strokePx, cap = StrokeCap.Round),
                        topLeft    = topLeft,
                        size       = arcSize,
                    )
                }
            }
        },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = "${(animatedProgress * 100).toInt()}$percentMark",
                fontSize   = fontSize,
                lineHeight = fontSize,
                letterSpacing = 0.asp,
                fontWeight = FontWeight.Bold,
                color      = progressColor,
            )
            label?.let {
                Text(
                    text       = it,
                    fontSize   = fontSize * 0.8f,
                    lineHeight = fontSize * 0.8f,
                    letterSpacing = 0.2.asp,
                    fontWeight = FontWeight.Bold,
                    color      = progressColor.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Preview(name = "Progress Ring · Light", showBackground = true)
@Preview(name = "Progress Ring · Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun CircularProgressRingPreview() {
    MaterialTheme {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressRing(progress = 0.78f, progressColor = MaterialTheme.colorScheme.primary, label = "DUE", modifier = Modifier.size(64.dp))
                CircularProgressRing(progress = 0.45f, progressColor = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(64.dp))
                CircularProgressRing(progress = 1f,    progressColor = MaterialTheme.colorScheme.secondary, label = "DONE", modifier = Modifier.size(64.dp))
                CircularProgressRing(progress = 0.60f, progressColor = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
            }
        }
    }
}
