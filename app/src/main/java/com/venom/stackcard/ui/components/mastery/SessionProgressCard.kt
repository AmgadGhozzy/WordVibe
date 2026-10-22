package com.venom.stackcard.ui.components.mastery

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.venom.domain.model.AppTheme
import com.venom.domain.model.WordProgressState
import com.venom.wordvibe.R
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp
import com.venom.wordvibe.ui.designsystem.WordVibeTheme
import com.venom.designsystem.theme.synapse

@Composable
fun SessionProgressCard(
    state: WordProgressState,
    modifier: Modifier = Modifier
) {
    val semantic = MaterialTheme.synapse.semantic

    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 20,
        contentPadding = 16
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.adp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.adp)) {
                    Text(
                        text = stringResource(R.string.session_progress_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = run {
                            val words = pluralStringResource(R.plurals.session_words_reviewed, state.completed, state.completed)
                            if (state.sessionDurationMs <= 0) {
                                words
                            } else {
                                val mins = (state.sessionDurationMs / 60000).toInt()
                                val secs = ((state.sessionDurationMs % 60000) / 1000).toInt()
                                val duration = if (mins > 0) {
                                    stringResource(R.string.duration_min_sec_short, mins, secs)
                                } else {
                                    stringResource(R.string.duration_sec_short, secs)
                                }
                                stringResource(R.string.format_dot_value, words, duration)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.adp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    XpBadge(xp = state.xpEarned)
                    StreakIndicator(streak = state.currentStreak)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
StatItem(
                    icon = R.drawable.ic_seal_check,
                    value = state.mastered.toString(),
                    label = stringResource(R.string.stat_mastered),
                    color = semantic.success
                )
                StatItem(
                    icon = R.drawable.ic_book_open,
                    value = state.learning.toString(),
                    label = stringResource(R.string.stat_learning),
                    color = semantic.gold
                )
                StatItem(
                    icon = R.drawable.ic_retry,
                    value = state.needsReview.toString(),
                    label = stringResource(R.string.stat_review),
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (state.xpToNextLevel > 0) {
                XpProgressBar(current = state.xpEarned, max = state.xpToNextLevel)
            }

            if (state.totalSessionCount > 0) {
                Text(
                    text = stringResource(R.string.stat_session_n, state.totalSessionCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun XpBadge(
    xp: Int,
    modifier: Modifier = Modifier
) {
    val xpColor = MaterialTheme.colorScheme.secondary

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.adp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                )
            )
            .border(1.adp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), RoundedCornerShape(10.adp))
            .padding(horizontal = 10.adp, vertical = 6.adp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.adp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_lightning),
            contentDescription = null,
            modifier = Modifier.size(14.adp),
            tint = xpColor
        )
        Text(
            text = "+$xp",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 12.asp
            ),
            color = xpColor
        )
Text(
            text = stringResource(R.string.stat_xp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.asp
            ),
            color = xpColor.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun XpProgressBar(
    current: Int,
    max: Int,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = (current.toFloat() / max).coerceIn(0f, 1f),
        animationSpec = tween(800, easing = FastOutSlowInEasing)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.adp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
Text(
                text = stringResource(R.string.stat_level_progress),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.stat_xp_progress, current, max),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.adp)
                .clip(RoundedCornerShape(3.adp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.adp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)
                        )
                    )
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: Int,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.adp)
    ) {
        Box(
            modifier = Modifier
                .size(40.adp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(20.adp),
                tint = color
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StreakIndicator(
    streak: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "flame")
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            tween(500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "scale"
    )
    val flameRotation by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(300), RepeatMode.Reverse),
        label = "rotate"
    )

    val flameColor = when {
        streak >= 30 -> MaterialTheme.colorScheme.error
        streak >= 14 -> MaterialTheme.colorScheme.tertiary
        streak >= 7 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.tertiary
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.adp))
            .background(flameColor.copy(alpha = 0.12f))
            .border(1.adp, flameColor.copy(alpha = 0.25f), RoundedCornerShape(10.adp))
            .padding(horizontal = 10.adp, vertical = 6.adp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.adp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_flame),
            contentDescription = null,
            modifier = Modifier
                .size(16.adp)
                .scale(flameScale)
                .rotate(flameRotation),
            tint = flameColor
        )
        Text(
            text = "$streak",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
            color = flameColor
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun SessionProgressCardPreview() {
    WordVibeTheme(darkTheme = true) {
        SessionProgressCard(
            state = WordProgressState(
                completed = 10,
                total = 10,
                mastered = 6,
                learning = 3,
                needsReview = 1,
                currentStreak = 7,
                xpEarned = 85,
                xpToNextLevel = 100,
                sessionDurationMs = 185000,
                totalSessionCount = 12
            ),
            modifier = Modifier.padding(16.adp)
        )
    }
}


