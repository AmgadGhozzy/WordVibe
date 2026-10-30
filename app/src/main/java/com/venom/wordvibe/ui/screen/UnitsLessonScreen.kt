package com.venom.wordvibe.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.venom.domain.model.WordMaster
import com.venom.ui.components.other.GlassCard
import com.venom.wordvibe.R
import com.venom.wordvibe.ui.designsystem.Spacing
import com.venom.wordvibe.ui.viewmodel.RecallLesson
import com.venom.wordvibe.ui.viewmodel.SynonymChallenge
import com.venom.wordvibe.ui.viewmodel.UnitsLessonViewModel
import com.venom.wordvibe.ui.viewmodel.recallLessonFor

/**
 * Phase 1 deep-dive: the untracked preview lesson for one EDITORIAL_CHAIN unit
 * (app03 §02). Read-only by design — nothing here writes progress, attempts,
 * or XP; the read-only note states it on screen.
 */
@Composable
fun UnitsLessonRoute(
    topic: String,
    onBack: () -> Unit,
    viewModel: UnitsLessonViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    UnitsLessonContent(topic = topic, isLoading = state.isLoading, words = state.words, synonymChallenges = state.synonymChallenges, onBack = onBack)
}

@Composable
private fun UnitsLessonContent(
    topic: String,
    isLoading: Boolean,
    words: List<WordMaster>,
    synonymChallenges: Map<Int, SynonymChallenge>,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("units_lesson_back"),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Column(modifier = Modifier.padding(start = Spacing.xs)) {
                Text(
                    text = stringResource(unitTopicTitleRes(topic)),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.units_lesson_read_only),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        when {
            isLoading -> Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            words.isEmpty() -> Text(
                text = stringResource(R.string.units_lesson_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.lg),
            )

            else -> words.forEachIndexed { index, word ->
                LessonWordCard(word = word, challenge = synonymChallenges[word.id])
                if (index != words.lastIndex) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
            }
        }
    }
}

@Composable
private fun LessonWordCard(
    word: WordMaster,
    challenge: SynonymChallenge?,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
        tint = null,
        showBorder = false,
        showShadow = false,
        contentPadding = Spacing.md,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.wordEn,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    posLabelRes(word.pos)?.let { posRes ->
                        Text(
                            text = stringResource(posRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (word.phoneticUs.isNotBlank()) {
                        Text(
                            text = word.phoneticUs,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(
                text = word.arabicAr,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = Spacing.sm),
            )
        }

        Text(
            text = word.definitionAr ?: word.definitionEn,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        word.definitionAr?.let {
            Text(
                text = word.definitionEn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        word.mnemonicAr?.takeIf { it.isNotBlank() }?.let { hook ->
            Text(
                text = hook,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }

        word.productionExpected?.takeIf { it.isNotBlank() }?.let { expected ->
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = stringResource(R.string.units_lesson_production),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = expected,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            word.examples[word.cefrLevel]?.takeIf { it.isNotBlank() }?.let { example ->
                Text(
                    text = example,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val recall = recallLessonFor(word)
        if (recall != null) {
            LessonRecallBlock(recall)
        }
        if (challenge != null) {
            LessonSynonymBlock(challenge)
        }
    }
}

@Composable
private fun LessonRecallBlock(
    recall: RecallLesson,
    modifier: Modifier = Modifier,
) {
    var revealed by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.units_lesson_recall_title),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = recall.gapped,
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
        )
        if (revealed) {
            Text(
                text = recall.answer,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Text(
                text = stringResource(R.string.units_lesson_blank_hint, recall.hintFirstLetter),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = { revealed = true },
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Text(text = stringResource(R.string.spelling_reveal_answer))
            }
        }
    }
}

@Composable
private fun LessonSynonymBlock(
    challenge: SynonymChallenge,
    modifier: Modifier = Modifier,
) {
    var picked by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.units_lesson_synonym_title),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            challenge.options.forEach { option ->
                val isCorrect = option.equals(challenge.correct, ignoreCase = true)
                val isPicked = picked != null && option == picked
                val borderColor = when {
                    picked == null -> MaterialTheme.colorScheme.outlineVariant
                    isCorrect -> MaterialTheme.colorScheme.primary
                    isPicked -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.outlineVariant
                }
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isPicked && !isCorrect) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                        .clickable { picked = option }
                        .padding(horizontal = Spacing.sm),
                )
            }
        }
    }
}
