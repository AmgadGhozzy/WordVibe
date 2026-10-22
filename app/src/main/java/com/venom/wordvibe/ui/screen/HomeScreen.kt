package com.venom.wordvibe.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venom.domain.model.CefrLevel
import com.venom.domain.model.DashboardData
import com.venom.domain.model.RelatedWords
import com.venom.domain.model.WordFamily
import com.venom.domain.model.WordMaster
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
            Column {
                Text(
                    text = stringResource(R.string.home_greeting),
                    style = MaterialTheme.typography.headlineMedium,
                )
                dashboard?.let { data ->
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
                                text = pluralStringResource(R.plurals.home_streak_value, data.currentStreak, data.currentStreak),
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
                                text = stringResource(R.string.home_freeze_balance, data.streakFreezes),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.home_freeze_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        dashboard?.let { data ->
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Text(
                            text = stringResource(R.string.home_today_session),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = pluralStringResource(R.plurals.home_due_label, data.totalWordsNeedsReview, data.totalWordsNeedsReview),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = pluralStringResource(R.plurals.home_new_label, newWordsCount, newWordsCount),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.home_estimated_time, estimatedMinutes),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
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
                AttentionRow(word = word)
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
    }
}

@Composable
private fun AttentionRow(word: WordMaster) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = LocalWordStateColors.current.isDueContainer.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                .height(MinTouchTarget),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = word.wordEn, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = word.arabicAr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            WordStateChip(
                style = WordStateChipStyle.DUE,
                label = stringResource(R.string.state_due),
            )
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
