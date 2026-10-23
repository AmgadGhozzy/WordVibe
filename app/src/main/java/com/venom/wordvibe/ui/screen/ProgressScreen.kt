package com.venom.wordvibe.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venom.domain.model.AppTheme
import com.venom.domain.model.DashboardData
import com.venom.wordvibe.R
import com.venom.wordvibe.ui.designsystem.ProgressRing
import com.venom.wordvibe.ui.designsystem.Spacing
import com.venom.wordvibe.ui.designsystem.StatCard
import com.venom.wordvibe.ui.designsystem.WordStateChipStyle
import com.venom.wordvibe.ui.designsystem.WordStateChip
import com.venom.wordvibe.ui.designsystem.WordVibeTheme
import com.venom.wordvibe.ui.viewmodel.ProgressViewModel
import androidx.compose.foundation.lazy.grid.GridItemSpan

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
        appTheme = state.appTheme,
        autoPronounce = state.autoPronounce,
        onUpdateDailyGoal = viewModel::updateDailyGoal,
        onUpdateAppTheme = viewModel::updateAppTheme,
        onUpdateAutoPronounce = viewModel::updateAutoPronounce,
    )
}

@Composable
fun ProgressContent(
    isLoading: Boolean,
    dashboard: DashboardData?,
    newWordsCount: Int,
    knownWordsCount: Int,
    modifier: Modifier = Modifier,
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

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item(span = { GridItemSpan(2) }) {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProgressRing(
                        progress = dashboard.levelProgress,
                        modifier = Modifier.padding(end = Spacing.lg),
                    ) {
                        Text(
                            text = stringResource(R.string.percent_value, (dashboard.levelProgress * 100).toInt()),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pluralStringResource(R.plurals.home_streak_value, dashboard.currentStreak, dashboard.currentStreak),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(
                                R.string.format_label_value,
                                stringResource(R.string.stat_best),
                                dashboard.bestStreak,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        )
                        Text(
                            text = stringResource(
                                R.string.format_label_value,
                                stringResource(R.string.stat_daily_goal),
                                stringResource(R.string.stat_xp_progress, dashboard.dailyGoalProgress, dashboard.dailyGoalTarget),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        )
                    }
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                }
            }
        }

        item(span = { GridItemSpan(2) }) {
            Text(
                text = stringResource(R.string.progress_words_by_state),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Word states in progression order. "Learning" excludes KNOWN words
        // (DashboardData's learning figure is LEARNING ∪ KNOWN).
        val stateCards = listOf(
            Triple(WordStateChipStyle.NEW, R.string.state_new, newWordsCount),
            Triple(WordStateChipStyle.LEARNING, R.string.state_learning,
                dashboard.totalWordsLearning - knownWordsCount),
            Triple(WordStateChipStyle.KNOWN, R.string.state_known, knownWordsCount),
            Triple(WordStateChipStyle.MASTERED, R.string.state_mastered, dashboard.totalWordsMastered),
        )
        items(stateCards) { (style, labelRes, value) ->
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(text = "$value", style = MaterialTheme.typography.headlineSmall)
                    WordStateChip(style = style, label = stringResource(labelRes))
                }
            }
        }

        // Scheduling, not a word state: due count gets its own line.
        item(span = { GridItemSpan(2) }) {
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
                ) {
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

        // Activity totals are not word states — plain labels, no chips.
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
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = stringResource(R.string.settings_daily_goal_desc),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    options.forEach { (goalXp, labelRes) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(
                                    selected = dashboard.dailyGoalTarget == goalXp,
                                    onClick = {
                                        onUpdateDailyGoal(goalXp)
                                        showSettingsDialog = false
                                    },
                                    role = Role.RadioButton,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = dashboard.dailyGoalTarget == goalXp,
                                onClick = null,
                            )
                            Text(
                                text = stringResource(labelRes, goalXp),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = Spacing.sm),
                            )
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = stringResource(R.string.settings_theme_label),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    listOf(AppTheme.SYSTEM, AppTheme.LIGHT, AppTheme.DARK).forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(
                                    selected = appTheme == theme,
                                    onClick = {
                                        onUpdateAppTheme(theme)
                                    },
                                    role = Role.RadioButton,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = appTheme == theme,
                                onClick = null,
                            )
                            Text(
                                text = stringResource(theme.title),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = Spacing.sm),
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .toggleable(
                                value = autoPronounce,
                                onValueChange = onUpdateAutoPronounce,
                                role = Role.Switch,
                            ),
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
