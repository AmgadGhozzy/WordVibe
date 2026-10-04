package com.venom.wordvibe.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venom.domain.model.CefrLevel
import com.venom.domain.model.RelatedWords
import com.venom.domain.model.WordFamily
import com.venom.domain.model.WordMaster
import com.venom.wordvibe.R
import com.venom.wordvibe.ui.designsystem.EmptyState
import com.venom.wordvibe.ui.designsystem.LocalWordStateColors
import com.venom.wordvibe.ui.designsystem.MinTouchTarget
import com.venom.wordvibe.ui.designsystem.Spacing
import com.venom.wordvibe.ui.designsystem.WordStateChip
import com.venom.wordvibe.ui.designsystem.WordStateChipStyle
import com.venom.wordvibe.ui.designsystem.WordVibeTheme
import com.venom.wordvibe.ui.viewmodel.SpellingQueueViewModel
import com.venom.lingospell.presentation.SpellingGameScreen

/**
 * Phase 2 writing tab: one queue, production-weak words first (ADR-0008).
 * Picking a word opens the typing editor fullscreen; closing returns to the
 * refreshed queue.
 */
@Composable
fun SpellingQueueRoute(viewModel: SpellingQueueViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SpellingQueueContent(
        isLoading = state.isLoading,
        items = state.items,
        selectedWord = state.selectedWord,
        onPick = viewModel::pick,
        onDismissEditor = viewModel::dismissEditor,
    )
}

@Composable
fun SpellingQueueContent(
    isLoading: Boolean,
    items: List<SpellingQueueViewModel.QueueItem>,
    selectedWord: WordMaster?,
    onPick: (WordMaster) -> Unit,
    onDismissEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (items.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.write_queue_empty_title),
                body = stringResource(R.string.write_queue_empty_body),
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.write_queue_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.xs),
                    )
                }
                items(items, key = { it.word.id }) { item ->
                    QueueRow(item) { onPick(item.word) }
                }
            }
        }

        selectedWord?.let { word ->
            SpellingGameScreen(
                customWord = word,
                onNavigateBack = onDismissEditor,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun QueueRow(item: SpellingQueueViewModel.QueueItem, onClick: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (item.productionWeak) {
                LocalWordStateColors.current.isDueContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                .height(MinTouchTarget),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.word.wordEn, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = item.word.arabicAr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            WordStateChip(
                style = if (item.isDue) WordStateChipStyle.DUE else WordStateChipStyle.LEARNING,
                label = stringResource(if (item.isDue) R.string.state_due else R.string.state_learning),
            )
        }
    }
}

@Preview(name = "Spelling queue — Arabic RTL", showBackground = true)
@Composable
private fun SpellingQueueContentPreview() {
    WordVibeTheme {
        SpellingQueueContent(
            isLoading = false,
            items = listOf(
                SpellingQueueViewModel.QueueItem(previewWord(1, "substantial"), isDue = true, productionWeak = true),
                SpellingQueueViewModel.QueueItem(previewWord(2, "inevitable"), isDue = false, productionWeak = false),
            ),
            selectedWord = null,
            onPick = {},
            onDismissEditor = {},
        )
    }
}

private fun previewWord(id: Int, wordEn: String): WordMaster = WordMaster(
    id = id,
    wordEn = wordEn,
    pos = "adjective",
    cefrLevel = CefrLevel.B2,
    fromOxford = 1,
    rank = 1000 + id,
    frequency = 3,
    difficultyScore = 5,
    phoneticUs = "test",
    phoneticUk = null,
    phoneticAr = "اختبار",
    translit = "test",
    syllabify = null,
    definitionEn = "Test definition.",
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
