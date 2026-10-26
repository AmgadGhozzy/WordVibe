package com.venom.quiz.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.venom.wordvibe.R
import com.venom.domain.model.CefrLevel
import com.venom.domain.model.QuizMode
import com.venom.domain.model.QuizQuestionUi
import com.venom.domain.model.QuizUiState
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp
import com.venom.ui.components.other.GlassThickness
import com.venom.ui.components.other.GradientGlassCard
import com.venom.wordvibe.core.theme.tokens.getCefrColorScheme
import com.venom.wordvibe.core.theme.tokens.getDifficultyTheme

/**
 * Adaptive quiz card — renders MINI (flashcard), FULL (exam), or PLACEMENT mode.
 */
@Composable
fun AdaptiveQuizCard(
    state: QuizUiState,
    onSelect: (String) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    embedded: Boolean = false
) {
    if (state.error != null) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.adp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.error_something_wrong),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.adp))
                androidx.compose.material3.TextButton(
                    onClick = onBack,
                    modifier = Modifier.size(width = 120.adp, height = 48.adp)
                ) {
                    Text(text = stringResource(R.string.action_close))
                }
            }
        }
        return
    }
    val question = state.currentQuestion ?: return
    val isMini = state.mode is QuizMode.Flashcard
    val isPlacement = state.mode is QuizMode.Placement
    val useCardFrontStyle = embedded || isMini
    val flashcardInput = (question as? QuizQuestionUi.FlashcardQuestion)?.input
    val cefrLevel = flashcardInput?.cefrLevel?.let { level ->
        runCatching { CefrLevel.valueOf(level.uppercase()) }.getOrNull()
    } ?: CefrLevel.B1
    val difficultyTheme = flashcardInput?.difficulty?.let { getDifficultyTheme(it) }
        ?: getDifficultyTheme(5)
    val cefrColors = getCefrColorScheme(cefrLevel)
    val cardShape = when {
        useCardFrontStyle -> RoundedCornerShape(32.adp)
        else -> RoundedCornerShape(0.adp)
    }
    val verticalPadding = if (useCardFrontStyle) 20.adp else 12.adp
    val horizontalPadding = if (useCardFrontStyle) 20.adp else 16.adp
    val tonalElevation = if (useCardFrontStyle) 0.adp else 0.adp

    val content: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (useCardFrontStyle) {
                            Modifier
                        } else {
                            Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding)
                        }
                    ),
                verticalArrangement = Arrangement.spacedBy(16.adp)
            ) {
                when {
                    isMini -> MiniHeader(cefrColors = cefrColors)
                    isPlacement -> PlacementHeader(state = state, onBack = onBack)
                    else -> FullHeader(state = state, onBack = onBack)
                }

                QuestionSection(prompt = question.prompt, isMini = isMini)

                OptionsList(
                    options = question.options,
                    selectedOption = state.selectedOption,
                    isAnswered = state.isAnswered,
                    correctAnswer = question.correctAnswer,
                    onOptionSelected = { if (!state.isAnswered) onSelect(it) }
                )

                if (!isMini) Spacer(Modifier.weight(1f))
            }

            if (!isMini) {
                AnimatedVisibility(
                    visible = state.isAnswered,
                    enter = slideInVertically { it / 2 } + fadeIn(),
                    exit = slideOutVertically { it / 2 } + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.adp)
                ) {
                    NextButton(onClick = onSkip)
                }
            }
        }
    }

    if (useCardFrontStyle) {
        GradientGlassCard(
            modifier = modifier.fillMaxSize(),
            thickness = GlassThickness.UltraThick,
            gradientColors = listOf(
                MaterialTheme.colorScheme.primary,
                difficultyTheme.background
            ),
            gradientAlpha = 0.05f,
            showBorder = true,
            showShadow = true,
            borderColor = difficultyTheme.border,
            shape = cardShape,
            contentPadding = 20.adp
        ) {
            content()
        }
    } else {
        Surface(
            modifier = modifier.fillMaxSize(),
            shape = cardShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = tonalElevation
        ) {
            content()
        }
    }
}

// Mini Header 

@Composable
private fun MiniHeader(cefrColors: com.venom.wordvibe.core.theme.tokens.CefrColorScheme) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.quiz_quick_quiz_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = cefrColors.content
        )
    }
}

// Placement Header 

@Composable
private fun PlacementHeader(state: QuizUiState, onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.adp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.adp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = colorScheme.onSurface,
                    modifier = Modifier.size(18.adp)
                )
            }

            Surface(
                shape = RoundedCornerShape(20.adp),
                color = colorScheme.primaryContainer
            ) {
                val label = state.placementBandLabel
                val cefr = state.cefr
                val band = state.placementBand
                val text = if (band > 0 && cefr.isNotEmpty()) {
                    stringResource(R.string.placement_band_label, band, cefr)
                } else {
                    label
                }
                Text(
                    text = text,
                    modifier = Modifier.padding(horizontal = 14.adp, vertical = 6.adp),
                    color = colorScheme.onPrimaryContainer,
                    fontSize = 13.asp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = stringResource(R.string.score_correct_of_total, state.correctCount, state.totalQuestions),
                color = colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }

        // Band progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.adp)
                .clip(RoundedCornerShape(3.adp))
                .background(colorScheme.surfaceContainerHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(state.progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.adp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(colorScheme.primary, colorScheme.tertiary)
                        )
                    )
            )
        }
    }
}

// Full Header 

@Composable
private fun FullHeader(state: QuizUiState, onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.adp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.adp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = colorScheme.onSurface,
                    modifier = Modifier.size(18.adp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.adp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScoreDisplay(score = state.score.toInt())
                StreakDisplay(streak = state.streak)
                HeartsDisplay(hearts = state.hearts)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.adp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.quiz_question_index, state.currentIndex + 1, state.totalQuestions),
                    color = colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                TimerChip(state.timeRemaining)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.adp)
                    .clip(RoundedCornerShape(3.adp))
                    .background(colorScheme.surfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(state.progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.adp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(colorScheme.primary, colorScheme.tertiary)
                            )
                        )
                )
            }
        }
    }
}

// Question Section 

@Composable
private fun QuestionSection(prompt: String, isMini: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.adp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.adp
    ) {
        Text(
            text = prompt,
            fontSize = if (isMini) 24.asp else 28.asp,
            lineHeight = if (isMini) 32.asp else 36.asp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isMini) 16.adp else 20.adp)
        )
    }
}

