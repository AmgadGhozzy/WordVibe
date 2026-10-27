package com.venom.wordvibe.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venom.domain.model.AppTheme
import com.venom.domain.model.DashboardData
import com.venom.domain.model.UserLevel
import com.venom.wordvibe.R
import com.venom.wordvibe.ui.designsystem.LocalWordStateColors
import com.venom.wordvibe.ui.designsystem.ProgressRing
import com.venom.wordvibe.ui.designsystem.Spacing
import com.venom.wordvibe.ui.designsystem.StatCard
import com.venom.wordvibe.ui.designsystem.WordStateChip
import com.venom.wordvibe.ui.designsystem.WordStateChipStyle
import com.venom.wordvibe.ui.designsystem.WordVibeTheme
import com.venom.ui.components.other.GlassCard
import com.venom.wordvibe.ui.viewmodel.ProgressViewModel

/* ───── Level name → existing string resource ───── */

@StringRes
private fun userLevelTitleRes(level: UserLevel): Int = when (level) {
    UserLevel.NEWCOMER -> R.string.user_level_newcomer
    UserLevel.EXPLORER -> R.string.user_level_explorer
    UserLevel.LEARNER -> R.string.user_level_learner
    UserLevel.APPRENTICE -> R.string.user_level_apprentice
    UserLevel.SCHOLAR -> R.string.user_level_scholar
    UserLevel.ADEPT -> R.string.user_level_adept
    UserLevel.EXPERT -> R.string.user_level_expert
    UserLevel.MASTER -> R.string.user_level_master
    UserLevel.CHAMPION -> R.string.user_level_champion
    UserLevel.LEGEND -> R.string.user_level_legend
    UserLevel.POLYGLOT -> R.string.user_level_polyglot
    UserLevel.SAGE -> R.string.user_level_sage
}

/* ───── Route (unchanged signature) ───── */

/**
 * Phase 1 progress tab: streak, daily goal ring, word-state classification, and
 * activity totals — every number comes straight from the existing SQL aggregates
 * (DashboardData). No new queries, no contract surface.
 */
@Composable
fun ProgressRoute(viewModel: ProgressViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProgressContent(
        isLoading = state.isLoading,
        dashboard = state.dashboard,
        newWordsCount = state.newWordsCount,
        knownWordsCount = state.knownWordsCount,
        reviewForecast = state.reviewForecast,
        scheduledToday = state.scheduledToday,
        appTheme = state.appTheme,
        autoPronounce = state.autoPronounce,
        onUpdateDailyGoal = viewModel::updateDailyGoal,
        onUpdateAppTheme = viewModel::updateAppTheme,
        onUpdateAutoPronounce = viewModel::updateAutoPronounce,
    )
}

/* ───── Content (existing params, two optional additions) ───── */

@Composable
fun ProgressContent(
    isLoading: Boolean,
    dashboard: DashboardData?,
    newWordsCount: Int,
    knownWordsCount: Int,
    modifier: Modifier = Modifier,
    reviewForecast: List<Int> = emptyList(),
    scheduledToday: Int = 0,
    appTheme: AppTheme = AppTheme.SYSTEM,
    autoPronounce: Boolean = true,
    onUpdateDailyGoal: (Int) -> Unit = {},
    onUpdateAppTheme: (AppTheme) -> Unit = {},
    onUpdateAutoPronounce: (Boolean) -> Unit = {},
) {
    if (isLoading || dashboard == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    val level = UserLevel.fromXp(dashboard.totalXp)

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        /* ── Hero card ── */
        item(span = { GridItemSpan(2) }) {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
                                ),
                            ),
                        ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.lg),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ProgressRing(
                                progress = dashboard.levelProgress,
                                modifier = Modifier.padding(end = Spacing.lg),
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.percent_value,
                                        (dashboard.levelProgress * 100).toInt(),
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                /* Level name badge */
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    contentColor = MaterialTheme.colorScheme.primary,
                                ) {
                                    Text(
                                        text = stringResource(userLevelTitleRes(level)),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(
                                            horizontal = Spacing.sm,
                                            vertical = 2.dp,
                                        ),
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(
                                        R.string.level_number_label,
                                        UserLevel.levelNumber(dashboard.totalXp),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                )
                            }
                            IconButton(
                                onClick = { showSettingsDialog = true },
                                modifier = Modifier.sizeIn(
                                    minWidth = 48.dp,
                                    minHeight = 48.dp,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = stringResource(R.string.settings_title),
                                )
                            }
                        }

                        /* Streak capsules — full card width below the level row */
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                            ) {
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.home_streak_value,
                                        dashboard.currentStreak,
                                        dashboard.currentStreak,
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    modifier = Modifier.padding(
                                        horizontal = Spacing.sm,
                                        vertical = Spacing.xs,
                                    ),
                                )
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.format_label_value,
                                        stringResource(R.string.stat_best),
                                        dashboard.bestStreak,
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    modifier = Modifier.padding(
                                        horizontal = Spacing.sm,
                                        vertical = Spacing.xs,
                                    ),
                                )
                            }
                        }

                        /* Daily goal progress bar */
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = stringResource(
                                R.string.format_label_value,
                                stringResource(R.string.stat_daily_goal),
                                stringResource(
                                    R.string.stat_xp_progress,
                                    dashboard.dailyGoalProgress,
                                    dashboard.dailyGoalTarget,
                                ),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = {
                                (dashboard.dailyGoalProgress.toFloat() / dashboard.dailyGoalTarget)
                                    .coerceIn(0f, 1f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                            strokeCap = StrokeCap.Round,
                            gapSize = 0.dp,
                            drawStopIndicator = {},
                        )
                    }
                }
            }
        }

        /* ── Word states header ── */
        item(span = { GridItemSpan(2) }) {
            Text(
                text = stringResource(R.string.progress_words_by_state),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        /* ── Four word-state cards with color stripe ── */
        // Word states in progression order. "Learning" excludes KNOWN words
        // (DashboardData's learning figure is LEARNING ∪ KNOWN).
        val stateCards = listOf(
            Triple(WordStateChipStyle.NEW, R.string.state_new, newWordsCount),
            Triple(WordStateChipStyle.LEARNING, R.string.state_learning,
                dashboard.totalWordsLearning - knownWordsCount),
            Triple(WordStateChipStyle.KNOWN, R.string.state_known, knownWordsCount),
            Triple(WordStateChipStyle.MASTERED, R.string.state_mastered, dashboard.totalWordsMastered),
        )

        /* ── Distribution bar (NEW → LEARNING → KNOWN → MASTERED) ── */
        item(span = { GridItemSpan(2) }) {
            val stateColors = LocalWordStateColors.current
            val totals = listOf(
                newWordsCount,
                dashboard.totalWordsLearning - knownWordsCount,
                knownWordsCount,
                dashboard.totalWordsMastered,
            )
            val totalWords = totals.sum()
            val barColors = listOf(
                stateColors.isNew,
                stateColors.isLearning,
                stateColors.isKnown,
                stateColors.isMastered,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            ) {
                totals.zip(barColors).forEach { (count, color) ->
                    val weight = when {
                        count > 0 -> count.toFloat()
                        totalWords == 0 -> 1f
                        else -> 0f
                    }
                    if (weight > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .fillMaxHeight()
                                .background(color),
                        )
                    }
                }
            }
        }

        items(stateCards) { (style, labelRes, value) ->
            val stateColors = LocalWordStateColors.current
            val stripeColor = when (style) {
                WordStateChipStyle.NEW -> stateColors.isNew
                WordStateChipStyle.LEARNING -> stateColors.isLearning
                WordStateChipStyle.KNOWN -> stateColors.isKnown
                WordStateChipStyle.MASTERED -> stateColors.isMastered
                WordStateChipStyle.DUE -> stateColors.isDue
            }
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(stripeColor),
                    )
                    Text(
                        text = "$value",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    WordStateChip(
                        style = style,
                        label = stringResource(labelRes),
                    )
                }
            }
        }

        /* ── Due card (scheduling, not a word state) ── */
        item(span = { GridItemSpan(2) }) {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = LocalWordStateColors.current.isDueContainer.copy(alpha = 0.35f),
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(LocalWordStateColors.current.isDue),
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = stringResource(R.string.state_due),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${dashboard.totalWordsNeedsReview}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        /* ── Review forecast (7-day sparkline) ── */
        item(span = { GridItemSpan(2) }) {
            ReviewForecastCard(
                forecast = reviewForecast,
                dueTodayCount = scheduledToday,
            )
        }

        /* ── Activity totals — plain labels, no chips ── */
        item {
            StatCard(
                label = stringResource(R.string.stat_days_active),
                value = "${dashboard.totalDaysActive}",
            )
        }
        item {
            StatCard(
                label = stringResource(R.string.stat_sessions),
                value = "${dashboard.totalSessionCount}",
                accent = MaterialTheme.colorScheme.tertiary,
            )
        }

        item(span = { GridItemSpan(2) }) {
            Text(
                text = stringResource(
                    R.string.format_dot_value,
                    stringResource(R.string.stat_today_xp, dashboard.todayXp),
                    stringResource(R.string.stat_total_xp_value, dashboard.totalXp),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }
    }

    /* ───── Settings dialog ───── */
    if (showSettingsDialog) {
        val options = listOf(
            30 to R.string.settings_goal_casual,
            50 to R.string.settings_goal_steady,
            100 to R.string.settings_goal_intense,
        )
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text(text = stringResource(R.string.settings_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {

                    /* Section 1 — Daily goal */
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    ) {
                        Column(modifier = Modifier.padding(Spacing.sm)) {
                            Text(
                                text = stringResource(R.string.settings_daily_goal_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = Spacing.xs),
                            )
                            options.forEach { (goalXp, labelRes) ->
                                val selected = dashboard.dailyGoalTarget == goalXp
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .then(
                                            if (selected) {
                                                Modifier.background(
                                                    MaterialTheme.colorScheme.primaryContainer.copy(
                                                        alpha = 0.45f,
                                                    ),
                                                )
                                            } else Modifier,
                                        )
                                        .selectable(
                                            selected = selected,
                                            onClick = {
                                                onUpdateDailyGoal(goalXp)
                                                showSettingsDialog = false
                                            },
                                            role = Role.RadioButton,
                                        )
                                        .padding(horizontal = Spacing.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = null,
                                    )
                                    Text(
                                        text = stringResource(labelRes, goalXp),
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(start = Spacing.sm),
                                    )
                                }
                            }
                        }
                    }

                    /* Section 2 — Theme */
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    ) {
                        Column(modifier = Modifier.padding(Spacing.sm)) {
                            Text(
                                text = stringResource(R.string.settings_theme_label),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = Spacing.xs),
                            )
                            listOf(
                                AppTheme.SYSTEM,
                                AppTheme.LIGHT,
                                AppTheme.DARK,
                            ).forEach { theme ->
                                val selected = appTheme == theme
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .then(
                                            if (selected) {
                                                Modifier.background(
                                                    MaterialTheme.colorScheme.primaryContainer.copy(
                                                        alpha = 0.45f,
                                                    ),
                                                )
                                            } else Modifier,
                                        )
                                        .selectable(
                                            selected = selected,
                                            onClick = { onUpdateAppTheme(theme) },
                                            role = Role.RadioButton,
                                        )
                                        .padding(horizontal = Spacing.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = null,
                                    )
                                    Text(
                                        text = stringResource(theme.title),
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(start = Spacing.sm),
                                    )
                                }
                            }
                        }
                    }

                    /* Section 3 — Auto-pronounce */
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .toggleable(
                                    value = autoPronounce,
                                    onValueChange = onUpdateAutoPronounce,
                                    role = Role.Switch,
                                )
                                .padding(Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.settings_auto_pronounce),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(checked = autoPronounce, onCheckedChange = null)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showSettingsDialog = false },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.settings_close))
                }
            },
        )
    }
}

/* ───── Review forecast card (GlassCard + sparkline) ───── */

@Composable
private fun ReviewForecastCard(
    forecast: List<Int>,
    dueTodayCount: Int,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        showBorder = false,
        contentPadding = Spacing.md,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.progress_forecast_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.progress_forecast_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$dueTodayCount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.progress_forecast_due_today),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        if (forecast.all { it == 0 }) {
            Text(
                text = stringResource(R.string.progress_forecast_empty),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.md),
            )
        } else {
            ReviewSparkline(
                dataPoints = forecast.map { it.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                forecast.indices.forEach { i ->
                    Text(
                        text = if (i == 0) {
                            stringResource(R.string.progress_forecast_day_today)
                        } else {
                            stringResource(R.string.progress_forecast_day_offset, i)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewSparkline(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val strokeWidth = 2.dp
    val dotRadius = 3.dp

    Canvas(modifier = modifier) {
        if (dataPoints.size < 2) return@Canvas

        val maxVal = dataPoints.max()
        val minVal = dataPoints.min()
        val range = (maxVal - minVal).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (dataPoints.size - 1)
        val topPad = size.height * 0.08f
        val drawHeight = size.height - topPad * 2f

        fun xAt(i: Int) = i * stepX
        fun yAt(v: Float) = topPad + drawHeight - ((v - minVal) / range) * drawHeight

        dataPoints.indices.forEach { i ->
            drawLine(
                color = gridLineColor,
                start = Offset(xAt(i), 0f),
                end = Offset(xAt(i), size.height),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
            )
        }

        val path = Path()
        dataPoints.forEachIndexed { i, v ->
            val x = xAt(i)
            val y = yAt(v)
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                val px = xAt(i - 1)
                val py = yAt(dataPoints[i - 1])
                val cpX = (px + x) / 2f
                path.cubicTo(cpX, py, cpX, y, x, y)
            }
        }

        // Gradient fill under the curve
        val fillPath = Path().apply {
            addPath(path)
            lineTo(xAt(dataPoints.lastIndex), size.height)
            lineTo(xAt(0), size.height)
            close()
        }
        drawPath(
            fillPath,
            Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.2f), Color.Transparent),
            ),
        )

        drawPath(
            path,
            lineColor,
            style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        dataPoints.forEachIndexed { i, v ->
            drawCircle(lineColor, dotRadius.toPx(), Offset(xAt(i), yAt(v)))
            drawCircle(Color.White, dotRadius.toPx() * 0.45f, Offset(xAt(i), yAt(v)))
        }
    }
}

@Preview(name = "Progress — Arabic RTL", showBackground = true)
@Composable
private fun ProgressContentPreview() {
    WordVibeTheme {
        ProgressContent(
            isLoading = false,
            newWordsCount = 74,
            knownWordsCount = 11,
            dashboard = DashboardData(
                currentStreak = 4,
                bestStreak = 9,
                totalXp = 640,
                todayXp = 45,
                levelProgress = 0.62f,
                dailyGoalProgress = 45,
                dailyGoalTarget = 50,
                totalWordsMastered = 8,
                totalWordsLearning = 14,
                totalWordsNeedsReview = 6,
                totalDaysActive = 11,
            ),
        )
    }
}
