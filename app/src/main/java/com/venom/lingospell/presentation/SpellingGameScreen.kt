@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.venom.lingospell.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venom.domain.model.WordMaster
import com.venom.lingospell.domain.FeedbackState
import com.venom.lingospell.presentation.components.HintButton
import com.venom.lingospell.presentation.components.MasteryDialog
import com.venom.lingospell.presentation.components.LetterBank
import com.venom.lingospell.presentation.components.StreakBar
import com.venom.lingospell.presentation.components.WordSlots
import com.venom.wordvibe.R
import com.venom.ui.components.buttons.CloseButton
import com.venom.ui.components.common.DynamicStyledText
import com.venom.ui.components.common.adp
import com.venom.ui.components.other.ConfettiAnimationType
import com.venom.ui.components.other.ConfettiView
import com.venom.ui.components.other.KonfettiStyle
import com.venom.designsystem.theme.synapse
import com.venom.ui.viewmodel.TTSViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SpellingGameScreen(
    onNavigateBack: () -> Unit,
    customWord: WordMaster,
    modifier: Modifier = Modifier,
    viewModel: SpellingGameViewModel = hiltViewModel(),
    ttsViewModel: TTSViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showHintSettings by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(false)
    }
    val onShowHintSettings = { showHintSettings = true }
    val onHideHintSettings = { showHintSettings = false }

    LaunchedEffect(customWord) {
        if (customWord.wordEn != state.currentWord?.wordEn) {
            viewModel.setCustomWord(customWord)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SpellingGameEvent.PlayTts -> ttsViewModel.toggle(event.text, event.languageTag)
                SpellingGameEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier.fillMaxSize()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.adp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val maxStreak = viewModel.appConfigProvider.spellingMaxStreak
                StreakBar(
                    streak = state.streak,
                    isMastered = state.streak >= maxStreak,
                    maxStreak = maxStreak
                )
                CloseButton(viewModel::onCloseClick)
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AnimatedContent(
                    targetState = state.currentWord?.arabicAr ?: "",
                    transitionSpec = {
                        (fadeIn() + scaleIn(initialScale = 0.95f)) togetherWith
                                (fadeOut() + scaleOut(targetScale = 0.95f))
                    },
                ) { arabic ->
                    val scale by animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
                    )
                    DynamicStyledText(
                        text = arabic,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        maxFontSize = 40,
                        minFontSize = 30,
                        lineHeight = 45,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(12.adp))

                // ADR-0005: the curated gapped recall sentence, when present.
                state.currentWord?.recallTarget?.let { cloze ->
                    Text(
                        text = cloze,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        ),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.adp),
                    )
                }

                Spacer(Modifier.height(16.adp))

                Box(
                    modifier = Modifier.height(48.adp).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = state.feedback,
                        transitionSpec = {
                            (fadeIn() + scaleIn(initialScale = 0.8f)) togetherWith
                                    (fadeOut() + scaleOut(targetScale = 0.8f))
                        },
                    ) { feedback ->
                        when (feedback) {
                            FeedbackState.SUCCESS -> Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.adp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_badge_check),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.adp),
                                    tint = MaterialTheme.synapse.semantic.success,
                                )
                                Text(
                                    text = stringResource(R.string.spelling_perfect),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.synapse.semantic.success,
                                )
                            }
                            FeedbackState.NEAR_MISS -> Text(
                                text = stringResource(R.string.spelling_near_miss),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                            else -> Text(
                                text = stringResource(R.string.spelling_translate_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.adp))

                // Hint counter: always visible; states whether the attempt counts.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.adp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.spelling_hints_used, state.hintLevel.value, state.maxHintsPerWord
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            R.string.spelling_attempts_left,
                            (3 - state.mistakes).coerceAtLeast(0)
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!state.attemptWillCount) {
                        Text(
                            text = stringResource(R.string.spelling_attempt_not_counted),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                Spacer(Modifier.height(8.adp))

                WordSlots(
                    slots = state.slots,
                    onSlotClick = viewModel::onSlotClick,
                    shakeTrigger = state.shakeTrigger,
                    feedback = state.feedback,
                )

                Spacer(Modifier.height(16.adp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.adp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.height(48.adp), contentAlignment = Alignment.Center) {
                        androidx.compose.animation.AnimatedVisibility(visible = state.showHintButton, enter = fadeIn(), exit = fadeOut()) {
                            HintButton(
                                hintLevel = state.hintLevel,
                                onHintClick = viewModel::onHintRequest,
                                enabled = true,
                            )
                        }
                    }
                    androidx.compose.material3.TextButton(
                        onClick = onShowHintSettings,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.spelling_hint_settings),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(8.adp))

                // "Reveal answer" = one recorded production failure (P-02).
                androidx.compose.animation.AnimatedVisibility(
                    visible = state.feedback != FeedbackState.SUCCESS,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    androidx.compose.material3.TextButton(
                        onClick = viewModel::onRevealAnswer,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.spelling_reveal_answer),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            LetterBank(
                letters = state.bank,
                onLetterClick = viewModel::onLetterClick,
                onClear = viewModel::onClearAll,
            )
        }

        if (state.feedback == FeedbackState.SUCCESS) {
            ConfettiView(
                animationType = ConfettiAnimationType.KONFETTI,
                konfettiStyle = KonfettiStyle.FESTIVE_BOTTOM,
            )
        }

        if (state.showCelebrationDialog) {
            MasteryDialog(
                word = state.currentWord?.wordEn ?: "",
                arabicWord = state.currentWord?.arabicAr ?: "",
                onContinue = viewModel::onContinueAfterCelebration,
            )
        }

        if (showHintSettings) {
            HintSettingsBottomSheet(
                onDismiss = onHideHintSettings,
                hintsEnabled = state.hintsEnabled,
                maxHintsPerWord = state.maxHintsPerWord,
                policyName = state.hintCreditPolicy,
                creditLimit = state.hintCreditLimit,
                onHintsEnabled = viewModel::setHintsEnabled,
                onMaxHints = viewModel::setMaxHintsPerWord,
                onPolicy = viewModel::setHintCreditPolicy,
                onLimit = viewModel::setHintCreditLimit,
            )
        }

    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun HintSettingsBottomSheet(
    onDismiss: () -> Unit,
    hintsEnabled: Boolean,
    maxHintsPerWord: Int,
    policyName: String,
    creditLimit: Int,
    onHintsEnabled: (Boolean) -> Unit,
    onMaxHints: (Int) -> Unit,
    onPolicy: (HintCreditPolicy) -> Unit,
    onLimit: (Int) -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        HintSettingsSheet(
            hintsEnabled, maxHintsPerWord, policyName, creditLimit,
            onHintsEnabled, onMaxHints, onPolicy, onLimit,
        )
    }
}

/**
 * Hint-budget settings (ADR-0009): enable/disable hints, per-word budget,
 * credit policy and its limit. Every change persists to DataStore immediately.
 */
@androidx.compose.runtime.Composable
private fun HintSettingsSheet(
    hintsEnabled: Boolean,
    maxHintsPerWord: Int,
    policyName: String,
    creditLimit: Int,
    onHintsEnabled: (Boolean) -> Unit,
    onMaxHints: (Int) -> Unit,
    onPolicy: (HintCreditPolicy) -> Unit,
    onLimit: (Int) -> Unit,
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.adp, vertical = 16.adp),
        verticalArrangement = Arrangement.spacedBy(12.adp),
    ) {
        Text(
            text = stringResource(R.string.spelling_hint_settings),
            style = MaterialTheme.typography.titleLarge,
        )

        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.spelling_hints_enabled),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            androidx.compose.material3.Switch(
                checked = hintsEnabled,
                onCheckedChange = onHintsEnabled,
            )
        }

        if (hintsEnabled) {
            Text(
                text = stringResource(R.string.spelling_max_hints),
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.adp)) {
                (0..3).forEach { n ->
                    androidx.compose.material3.FilterChip(
                        selected = maxHintsPerWord == n,
                        onClick = { onMaxHints(n) },
                        label = { Text("$n") },
                    )
                }
            }

            Text(
                text = stringResource(R.string.spelling_credit_policy),
                style = MaterialTheme.typography.bodyLarge,
            )
            androidx.compose.material3.RadioButton(
                selected = policyName == HintCreditPolicy.NO_CREDIT_IF_ANY_HINT.name,
                onClick = { onPolicy(HintCreditPolicy.NO_CREDIT_IF_ANY_HINT) },
            )
            Text(text = stringResource(R.string.spelling_policy_no_credit))
            androidx.compose.material3.RadioButton(
                selected = policyName == HintCreditPolicy.CREDIT_UP_TO_N.name,
                onClick = { onPolicy(HintCreditPolicy.CREDIT_UP_TO_N) },
            )
            Text(text = stringResource(R.string.spelling_policy_up_to_n))

            if (policyName == HintCreditPolicy.CREDIT_UP_TO_N.name) {
                Text(
                    text = stringResource(R.string.spelling_credit_limit),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.adp)) {
                    (0..3).forEach { n ->
                        androidx.compose.material3.FilterChip(
                            selected = creditLimit == n,
                            onClick = { onLimit(n) },
                            label = { Text("$n") },
                        )
                    }
                }
            }
        }

        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.height(24.adp)
        )
    }
}
