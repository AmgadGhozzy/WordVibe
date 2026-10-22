package com.venom.stackcard.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.venom.domain.model.AppTheme
import com.venom.domain.model.DashboardData
import com.venom.domain.model.UserLevel
import com.venom.wordvibe.R
import com.venom.stackcard.ui.components.mastery.GlassStatCard
import com.venom.stackcard.ui.components.mastery.GlassSurface
import com.venom.stackcard.ui.components.mastery.StreakIndicator
import com.venom.stackcard.ui.viewmodel.WordMasteryViewModel
import com.venom.ui.components.buttons.CloseButton
import com.venom.ui.components.common.adp
import com.venom.wordvibe.ui.designsystem.WordVibeTheme
import com.venom.designsystem.theme.synapse

@Composable
fun ProgressScreen(
    viewModel: WordMasteryViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val data = uiState.userProgress ?: DashboardData()

    LaunchedEffect(Unit) { viewModel.refreshProgress() }

    ProgressScreenContent(data = data, onBack = onBack, modifier = modifier)
}

@Composable
private fun ProgressScreenContent(
    data: DashboardData,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val level = UserLevel.fromXp(data.totalXp)

    BackHandler(
        enabled = true,
        onBack = onBack
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.adp),
        verticalArrangement = Arrangement.spacedBy(20.adp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.progress_screen_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            CloseButton(onBack)
        }

        LevelCard(
            level = level,
            totalXp = data.totalXp,
            levelProgress = data.levelProgress,
            xpToNextLevel = data.xpToNextLevel,
            todayXp = data.todayXp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.adp)
        ) {
GlassStatCard(
                title = stringResource(R.string.stat_words_learned),
                value = data.totalWordsLearned.toString(),
                icon = R.drawable.ic_book_open,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            GlassStatCard(
                title = stringResource(R.string.stat_mastered),
                value = data.totalWordsMastered.toString(),
                icon = R.drawable.ic_seal_check,
                color = MaterialTheme.synapse.semantic.success,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.adp)
        ) {
GlassStatCard(
                title = stringResource(R.string.stat_sessions),
                value = data.totalSessionCount.toString(),
                icon = R.drawable.ic_lightning,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
            GlassStatCard(
                title = stringResource(R.string.stat_days_active),
                value = data.totalDaysActive.toString(),
                icon = R.drawable.ic_calendar,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        StreakCard(currentStreak = data.currentStreak, bestStreak = data.bestStreak)

        if (data.totalTimeMs > 0) {
            TimeCard(totalTimeMs = data.totalTimeMs)
        }

        if (data.dailyGoalTarget > 0) {
            DailyGoalCard(
                progress = data.dailyGoalProgress,
                target = data.dailyGoalTarget,
                met = data.dailyGoalMet
            )
        }

        Spacer(modifier = Modifier.height(20.adp))
    }
}

@Composable
private fun LevelCard(
    level: UserLevel,
    totalXp: Int,
    levelProgress: Float,
    xpToNextLevel: Int,
    todayXp: Int,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val themeColor = MaterialTheme.colorScheme.primary

    val animatedProgress by animateFloatAsState(
        targetValue = levelProgress.coerceIn(0f, 1f),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "levelProgress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.adp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        themeColor.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            )
            .border(1.adp, themeColor.copy(alpha = 0.15f), RoundedCornerShape(20.adp))
            .padding(20.adp),
        verticalArrangement = Arrangement.spacedBy(16.adp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.adp)) {
                Text(
                    text = level.displayName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = themeColor
                )
                Text(
                    text = stringResource(R.string.level_number_label, UserLevel.levelNumber(totalXp)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.adp)
            ) {
Text(
                    text = stringResource(R.string.stat_total_xp_value, totalXp),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (todayXp > 0) {
                    Text(
                        text = stringResource(R.string.stat_today_xp, todayXp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.adp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.percent_value, (animatedProgress * 100).toInt()),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = themeColor
                )
                if (xpToNextLevel > 0) {
Text(
                        text = stringResource(R.string.stat_xp_to_next, xpToNextLevel),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.adp)
                    .clip(RoundedCornerShape(4.adp))
                    .background(
                        if (isDarkTheme) Color.White.copy(alpha = 0.1f)
                        else Color.Black.copy(alpha = 0.08f)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.adp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(themeColor, themeColor.copy(alpha = 0.7f))
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun StreakCard(
    currentStreak: Int,
    bestStreak: Int,
    modifier: Modifier = Modifier
) {
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.adp)) {
                Text(
                    text = stringResource(R.string.progress_current_streak_label),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = pluralStringResource(R.plurals.progress_best_streak, bestStreak, bestStreak),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            StreakIndicator(streak = currentStreak)
        }
    }
}

@Composable
private fun TimeCard(totalTimeMs: Long, modifier: Modifier = Modifier) {
    val hours = (totalTimeMs / 3600000).toInt()
    val minutes = ((totalTimeMs % 3600000) / 60000).toInt()
    val timeText = when {
        hours > 0 -> stringResource(R.string.duration_hours_minutes, hours, minutes)
        minutes > 0 -> stringResource(R.string.duration_minutes, minutes)
        else -> stringResource(R.string.duration_less_than_minute)
    }

    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.progress_total_study_time_label),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.adp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_clock),
                    contentDescription = null,
                    modifier = Modifier.size(16.adp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DailyGoalCard(
    progress: Int,
    target: Int,
    met: Boolean,
    modifier: Modifier = Modifier
) {
    val fraction = (progress.toFloat() / target).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "dailyGoal"
    )
    val goalColor = if (met) MaterialTheme.synapse.semantic.success else MaterialTheme.colorScheme.primary

    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.adp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
Text(
                    text = if (met) stringResource(R.string.stat_daily_goal_met) else stringResource(R.string.stat_daily_goal),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (met) MaterialTheme.synapse.semantic.success else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.stat_xp_progress, progress, target),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = goalColor
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.adp)
                    .clip(RoundedCornerShape(4.adp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.adp))
                        .background(goalColor)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun ProgressScreenPreview() {
    WordVibeTheme(darkTheme = true) {
        ProgressScreenContent(
            data = DashboardData(
                totalXp = 2450, todayXp = 85, level = UserLevel.ADEPT,
                levelProgress = 0.68f, xpToNextLevel = 350,
                totalWordsLearned = 156, totalWordsMastered = 42,
                currentStreak = 7, bestStreak = 12,
                totalSessionCount = 23, totalTimeMs = 5400000,
                totalDaysActive = 15, dailyGoalTarget = 50,
                dailyGoalProgress = 35, dailyGoalMet = false
            )
        )
    }
}


