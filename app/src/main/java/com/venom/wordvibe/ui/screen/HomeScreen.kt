package com.venom.wordvibe.ui.screen

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venom.data.repo.UserWordProgressRepository
import com.venom.domain.model.CefrLevel
import com.venom.domain.model.DashboardData
import com.venom.domain.model.RelatedWords
import com.venom.domain.model.WordFamily
import com.venom.domain.model.WordMaster
import com.venom.ui.components.other.CircularProgressRing
import com.venom.ui.components.other.GlassCard
import com.venom.ui.components.other.GlassThickness
import com.venom.ui.components.other.GlassTint
import com.venom.ui.components.other.GradientGlassCard
import com.venom.wordvibe.R
import com.venom.wordvibe.ui.designsystem.LocalWordStateColors
import com.venom.wordvibe.ui.designsystem.MinTouchTarget
import com.venom.wordvibe.ui.designsystem.PrimaryButton
import com.venom.wordvibe.ui.designsystem.Spacing
import com.venom.wordvibe.ui.designsystem.WordStateChip
import com.venom.wordvibe.ui.designsystem.WordStateChipStyle
import com.venom.wordvibe.ui.designsystem.WordVibeTheme
import com.venom.wordvibe.ui.viewmodel.HomeViewModel

/**
 * Phase 1 home: greeting + streak, today's session card (due/new/minutes), start,
 * and the needs-attention rail. Content is a pure composable; [HomeRoute] binds it
 * to Hilt and navigation.
 */
@Composable
fun HomeRoute(
    onStartSession: () -> Unit,
    onPractice: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(
        isLoading = state.isLoading,
        dashboard = state.dashboard,
        newWordsCount = state.newWordsCount,
        attentionWords = state.attentionWords,
        units = state.units,
        estimatedMinutes = viewModel.estimatedMinutes(
            due = state.dashboard?.totalWordsNeedsReview ?: 0,
            new = state.newWordsCount,
        ),
        onStartSession = onStartSession,
        onPractice = onPractice,
    )
}

@Composable
fun HomeContent(
    isLoading: Boolean,
    dashboard: DashboardData?,
    newWordsCount: Int,
    attentionWords: List<WordMaster>,
    estimatedMinutes: Int,
    onStartSession: () -> Unit,
    onPractice: () -> Unit,
    modifier: Modifier = Modifier,
    units: List<UserWordProgressRepository.UnitStatus> = emptyList(),
) {
    if (isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = stringResource(R.string.home_greeting),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                dashboard?.let { data ->
                    val animatedGoalProgress by animateIntAsState(
                        targetValue = data.dailyGoalProgress,
                        label = "dailyGoalProgressAnim"
                    )
                    val goalFraction = (data.dailyGoalProgress.toFloat() / data.dailyGoalTarget.coerceAtLeast(1))
                        .coerceIn(0f, 1f)

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = GlassThickness.Regular,
                        tint = GlassTint.Primary,
                        showBorder = true,
                        contentPadding = Spacing.md,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = Spacing.sm),
                                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Text(
                                            text = pluralStringResource(
                                                R.plurals.home_streak_value,
                                                data.currentStreak,
                                                data.currentStreak
                                            ),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.tertiary,
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.AcUnit,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.home_freeze_balance,
                                                data.streakFreezes
                                            ),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }

                                Text(
                                    text = stringResource(
                                        R.string.format_label_value,
                                        stringResource(R.string.stat_daily_goal),
                                        stringResource(
                                            R.string.stat_xp_progress,
                                            animatedGoalProgress,
                                            data.dailyGoalTarget
                                        )
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )

                                Text(
                                    text = stringResource(R.string.home_freeze_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            CircularProgressRing(
                                progress = goalFraction,
                                modifier = Modifier.size(56.dp),
                                strokeWidthDp = 6.dp,
                                glowEnabled = false,
                                progressColor = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        dashboard?.let { data ->
            item {
                GradientGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    gradientColors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    ),
                    gradientAlpha = 0.16f,
                    showOrbet = true,
                    orbetColor = MaterialTheme.colorScheme.primary,
                    showBorder = true,
                    contentPadding = Spacing.lg,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(R.string.home_today_session),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            ) {
                                Text(
                                    text = stringResource(R.string.home_estimated_time, estimatedMinutes),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                            ) {
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.home_due_label,
                                        data.totalWordsNeedsReview,
                                        data.totalWordsNeedsReview
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(Spacing.sm),
                                )
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                            ) {
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.home_new_label,
                                        newWordsCount,
                                        newWordsCount
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(Spacing.sm),
                                )
                            }
                        }

                        val dailyGoalRatio = (data.dailyGoalProgress.toFloat() / data.dailyGoalTarget.coerceAtLeast(1))
                            .coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { dailyGoalRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                        )

                        Spacer(modifier = Modifier.height(Spacing.xs))
                        PrimaryButton(
                            text = stringResource(R.string.home_start_button),
                            onClick = onStartSession,
                            leadingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                        )
                    }
                }
            }
        }

        item {
            PrimaryButton(
                text = stringResource(R.string.home_practice_button),
                onClick = onPractice,
            )
        }

        if (attentionWords.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.home_attention_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(attentionWords, key = { it.id }) { word ->
                AttentionRow(word = word, onPractice = onPractice)
            }
        } else {
            item {
                Text(
                    text = stringResource(R.string.home_attention_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (units.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.home_units_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            itemsIndexed(units, key = { _, unit -> unit.topic }) { index, unit ->
                UnitRow(index = index, unit = unit)
            }
        }
    }
}

@StringRes
fun posLabelRes(pos: String): Int? = when (pos.lowercase()) {
    "noun" -> R.string.pos_noun
    "verb" -> R.string.pos_verb
    "adj", "adjective" -> R.string.pos_adj
    "adv", "adverb" -> R.string.pos_adv
    else -> null
}

@StringRes
fun unitTopicTitleRes(topic: String): Int = when (topic.lowercase()) {
    "education" -> R.string.unit_topic_education
    "work" -> R.string.unit_topic_work
    "health" -> R.string.unit_topic_health
    "travel" -> R.string.unit_topic_travel
    "technology" -> R.string.unit_topic_technology
    "environment" -> R.string.unit_topic_environment
    "culture" -> R.string.unit_topic_culture
    "society" -> R.string.unit_topic_society
    "economy" -> R.string.unit_topic_economy
    "science" -> R.string.unit_topic_science
    else -> R.string.unit_topic_education
}

@Composable
private fun UnitRow(index: Int, unit: UserWordProgressRepository.UnitStatus) {
    val topicTitle = stringResource(unitTopicTitleRes(unit.topic))
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (unit.isOpen) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Safe tap */ },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                .heightIn(min = MinTouchTarget),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (unit.isOpen) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (unit.isOpen) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = topicTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (unit.isOpen) {
                    if (unit.usableCount > 0) {
                        val progressFraction = (unit.knownCount.toFloat() / unit.usableCount).coerceIn(0f, 1f)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                            )
                            Text(
                                text = stringResource(
                                    R.string.score_correct_of_total,
                                    unit.knownCount,
                                    unit.usableCount
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else if (unit.lockReason != null) {
                    Text(
                        text = stringResource(
                            R.string.unit_lock_reason,
                            unit.lockReason.remaining,
                            topicTitle,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (unit.isOpen) {
                WordStateChip(
                    style = WordStateChipStyle.KNOWN,
                    label = stringResource(R.string.unit_status_open),
                )
            } else {
                WordStateChip(
                    style = WordStateChipStyle.LEARNING,
                    label = stringResource(R.string.unit_status_locked),
                )
            }
        }
    }
}

@Composable
private fun AttentionRow(
    word: WordMaster,
    onPractice: () -> Unit = {},
) {
    var isExpanded by rememberSaveable(word.id) { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "expandArrowRotation"
    )

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = LocalWordStateColors.current.isDueContainer.copy(alpha = 0.5f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MinTouchTarget),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // Colored sidebar
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 36.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(LocalWordStateColors.current.isDue),
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        Text(
                            text = word.wordEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        posLabelRes(word.pos)?.let { posRes ->
                            Text(
                                text = stringResource(posRes),
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        ) {
                            Text(
                                text = word.cefrLevel.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        text = word.arabicAr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    WordStateChip(
                        style = WordStateChipStyle.DUE,
                        label = stringResource(R.string.state_due),
                    )
                    Icon(
                        imageVector = Icons.Filled.ExpandMore,
                        contentDescription = stringResource(if (isExpanded) R.string.cd_collapse else R.string.cd_expand),
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(arrowRotation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.xs, bottom = Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    )

                    val phoneticText = buildString {
                        if (word.phoneticUs.isNotBlank()) append("/${word.phoneticUs}/")
                        if (word.phoneticUs.isNotBlank() && word.phoneticAr.isNotBlank()) append("  •  ")
                        if (word.phoneticAr.isNotBlank()) append(word.phoneticAr)
                    }
                    if (phoneticText.isNotBlank()) {
                        Text(
                            text = phoneticText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    if (word.definitionEn.isNotBlank()) {
                        Text(
                            text = word.definitionEn,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    word.definitionAr?.takeIf { it.isNotBlank() }?.let { defAr ->
                        Text(
                            text = defAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    word.examples.values.firstOrNull()?.takeIf { it.isNotBlank() }?.let { exampleSentence ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "“$exampleSentence”",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(Spacing.sm),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Home — Arabic RTL", showBackground = true)
@Composable
private fun HomeContentPreview() {
    WordVibeTheme {
        HomeContent(
            isLoading = false,
            dashboard = DashboardData(currentStreak = 4, totalWordsNeedsReview = 7),
            newWordsCount = 12,
            attentionWords = listOf(previewWord()),
            estimatedMinutes = 9,
            onStartSession = {},
            onPractice = {},
        )
    }
}

private fun previewWord(): WordMaster = WordMaster(
    id = 7,
    wordEn = "obtain",
    pos = "verb",
    cefrLevel = CefrLevel.B1,
    fromOxford = 1,
    rank = 1000,
    frequency = 3,
    difficultyScore = 5,
    phoneticUs = "əbˈteɪn",
    phoneticUk = null,
    phoneticAr = "أوبتين",
    translit = "obtain",
    syllabify = null,
    definitionEn = "To get or acquire something.",
    definitionAr = null,
    usageNote = null,
    category = null,
    primarySense = null,
    semanticTags = emptyList(),
    register = "Neutral",
    mnemonicAr = null,
    wordFamily = WordFamily(),
    synonyms = emptyList(),
    antonyms = emptyList(),
    examples = emptyMap(),
    collocations = emptyList(),
    relatedWords = RelatedWords(emptyList(), emptyList()),
    arabicAr = "يحصل على",
)
