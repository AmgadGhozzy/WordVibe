@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.venom.wordvibe.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.write_queue_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = stringResource(R.string.write_count_label, items.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
    var isExpanded by rememberSaveable(item.word.id) { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "expandArrowRotation"
    )

    val sideBarColor = if (item.productionWeak) {
        LocalWordStateColors.current.isDue
    } else {
        MaterialTheme.colorScheme.primary
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (item.productionWeak) {
                LocalWordStateColors.current.isDueContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                        .background(sideBarColor),
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
                            text = item.word.wordEn,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontSize = 18.sp,
                            ),
                            fontWeight = FontWeight.Bold,
                        )
                        posLabelRes(item.word.pos)?.let { posRes ->
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
                                text = item.word.cefrLevel.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        text = item.word.arabicAr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    WordStateChip(
                        style = if (item.isDue) WordStateChipStyle.DUE else WordStateChipStyle.LEARNING,
                        label = stringResource(if (item.isDue) R.string.state_due else R.string.state_learning),
                    )
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
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
                        if (item.word.phoneticUs.isNotBlank()) append("/${item.word.phoneticUs}/")
                        if (item.word.phoneticUs.isNotBlank() && item.word.phoneticAr.isNotBlank()) append("  •  ")
                        if (item.word.phoneticAr.isNotBlank()) append(item.word.phoneticAr)
                    }
                    if (phoneticText.isNotBlank()) {
                        Text(
                            text = phoneticText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    if (item.word.definitionEn.isNotBlank()) {
                        Text(
                            text = item.word.definitionEn,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    item.word.definitionAr?.takeIf { it.isNotBlank() }?.let { defAr ->
                        Text(
                            text = defAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    item.word.examples.values.firstOrNull()?.takeIf { it.isNotBlank() }?.let { exampleSentence ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(MaterialTheme.colorScheme.primary),
                                )
                                Text(
                                    text = buildAnnotatedString {
                                        append("“")
                                        highlightWordInSentence(
                                            this,
                                            exampleSentence,
                                            item.word.wordEn,
                                            SpanStyle(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                            ),
                                        )
                                        append("”")
                                    },
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
