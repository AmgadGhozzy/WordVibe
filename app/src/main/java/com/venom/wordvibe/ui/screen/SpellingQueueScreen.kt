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
import com.venom.wordvibe.ui.designsystem.MinTouchTarget
import com.venom.wordvibe.ui.designsystem.Spacing
import com.venom.wordvibe.ui.designsystem.WordStateChip
import com.venom.wordvibe.ui.designsystem.WordStateChipStyle
import com.venom.wordvibe.ui.designsystem.WordVibeTheme
import com.venom.wordvibe.ui.viewmodel.SpellingQueueViewModel
import com.venom.lingospell.presentation.SpellingGameScreen

/**
 * Phase 1 writing tab: a real spelling queue — due words plus production-weak words.
 * Picking a word opens the existing spelling editor fullscreen; closing returns to
 * the refreshed queue. This replaces the duplicated flashcard route.
 */
@Composable
fun SpellingQueueRoute(viewModel: SpellingQueueViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SpellingQueueContent(
        isLoading = state.isLoading,
        dueWords = state.dueWords,
        weakWords = state.weakWords,
        selectedWord = state.selectedWord,
        onPick = viewModel::pick,
        onDismissEditor = viewModel::dismissEditor,
    )
}

@Composable
fun SpellingQueueContent(
    isLoading: Boolean,
    dueWords: List<WordMaster>,
    weakWords: List<WordMaster>,
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
        if (dueWords.isEmpty() && weakWords.isEmpty()) {
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
                if (dueWords.isNotEmpty()) {
                    item {
                        QueueSectionHeader(
                            title = stringResource(R.string.write_due_section),
                            count = dueWords.size,
                        )
                    }
                    items(dueWords, key = { "due-${it.id}" }) { word ->
                        QueueRow(word, chip = WordStateChipStyle.DUE) { onPick(word) }
                    }
                }
                if (weakWords.isNotEmpty()) {
                    item {
                        QueueSectionHeader(
                            title = stringResource(R.string.write_weak_section),
                            count = weakWords.size,
                        )
                    }
                    items(weakWords, key = { "weak-${it.id}" }) { word ->
                        QueueRow(word, chip = WordStateChipStyle.LEARNING) { onPick(word) }
                    }
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
private fun QueueSectionHeader(title: String, count: Int) {
    Text(
        text = "$title · " + stringResource(R.string.write_count_label, count),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.sm),
    )
}

@Composable
private fun QueueRow(
    word: WordMaster,
    chip: WordStateChipStyle,
    onClick: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
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
                Text(text = word.wordEn, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = word.arabicAr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            WordStateChip(style = chip, label = stringResource(chip.labelRes()))
        }
    }
}

private fun WordStateChipStyle.labelRes(): Int = when (this) {
    WordStateChipStyle.NEW -> R.string.state_new
    WordStateChipStyle.LEARNING -> R.string.state_learning
    WordStateChipStyle.KNOWN -> R.string.state_known
    WordStateChipStyle.MASTERED -> R.string.state_mastered
    WordStateChipStyle.DUE -> R.string.state_due
}

@Preview(name = "Spelling queue — Arabic RTL", showBackground = true)
@Composable
private fun SpellingQueueContentPreview() {
    WordVibeTheme {
        SpellingQueueContent(
            isLoading = false,
            dueWords = listOf(previewWord(1, "substantial"), previewWord(2, "significant")),
            weakWords = listOf(previewWord(3, "inevitable")),
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
