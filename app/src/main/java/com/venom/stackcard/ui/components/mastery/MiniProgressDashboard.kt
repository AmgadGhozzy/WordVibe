package com.venom.stackcard.ui.components.mastery

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.venom.domain.model.AppTheme
import com.venom.domain.model.DashboardData
import com.venom.domain.model.UserLevel
import com.venom.wordvibe.R
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp
import com.venom.designsystem.theme.SynapseTheme
import com.venom.designsystem.theme.synapse

@Composable
fun MiniProgressDashboard(
    data: DashboardData,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 14
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.adp)
        ) {
            MiniProgressRing(
                progress = data.levelProgress,
                levelNumber = UserLevel.levelNumber(data.totalXp),
                modifier = Modifier.size(52.adp)
            )

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
MiniStat(
                    value = data.totalWordsLearned.toString(),
                    label = stringResource(R.string.stat_words),
                    icon = R.drawable.ic_book_open,
                    color = MaterialTheme.colorScheme.primary
                )
                MiniStat(
                    value = data.currentStreak.toString(),
                    label = stringResource(R.string.stat_streak),
                    icon = R.drawable.ic_flame,
                    color = MaterialTheme.colorScheme.tertiary
                )
                MiniStat(
                    value = "+${data.todayXp}",
                    label = stringResource(R.string.stat_xp),
                    icon = R.drawable.ic_lightning,
                    color = MaterialTheme.colorScheme.secondary
                )
                MiniStat(
                    value = data.totalWordsMastered.toString(),
                    label = stringResource(R.string.stat_mastered),
                    icon = R.drawable.ic_seal_check,
                    color = MaterialTheme.synapse.semantic.success
                )
            }
        }
    }
}

@Composable
private fun MiniProgressRing(
    progress: Float,
    levelNumber: Int,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val themeColor = MaterialTheme.colorScheme.primary

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val strokeWidthDp = 5.adp
    Box(
        modifier = modifier
            .drawBehind {
                val strokeWidth = strokeWidthDp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val topLeft = Offset(
                    (size.width - radius * 2) / 2,
                    (size.height - radius * 2) / 2
                )
                val arcSize = Size(radius * 2, radius * 2)

                drawArc(
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.1f)
                    else Color.Black.copy(alpha = 0.08f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = topLeft,
                    size = arcSize
                )

                drawArc(
                    color = themeColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = topLeft,
                    size = arcSize
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
Text(
                text = stringResource(R.string.stat_level),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.asp,
                    fontWeight = FontWeight.Medium
                ),
                color = themeColor.copy(alpha = 0.7f)
            )
            Text(
                text = "$levelNumber",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black
                ),
                color = themeColor
            )
        }
    }
}

@Composable
private fun MiniStat(
    value: String,
    label: String,
    icon: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.adp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.adp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(12.adp),
                tint = color
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.asp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun MiniProgressDashboardPreview() {
    SynapseTheme(darkTheme = true) {
        MiniProgressDashboard(
            data = DashboardData(
                totalXp = 2450,
                todayXp = 85,
                level = UserLevel.ADEPT,
                levelProgress = 0.68f,
                xpToNextLevel = 350,
                totalWordsLearned = 156,
                totalWordsMastered = 42,
                currentStreak = 7,
                totalSessionCount = 23,
                totalDaysActive = 15
            ),
            modifier = Modifier.padding(16.adp)
        )
    }
}

