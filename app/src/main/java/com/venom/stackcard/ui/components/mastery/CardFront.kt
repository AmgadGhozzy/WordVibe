package com.venom.stackcard.ui.components.mastery

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venom.data.mock.MockWordData
import com.venom.domain.model.AppTheme
import com.venom.domain.model.WordMaster
import com.venom.ui.components.common.DynamicStyledText
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp
import com.venom.ui.components.other.GlassThickness
import com.venom.ui.components.other.GradientGlassCard
import com.venom.ui.viewmodel.TTSViewModel
import com.venom.wordvibe.R
import com.venom.wordvibe.ui.designsystem.WordVibeTheme
import com.venom.wordvibe.core.theme.tokens.getCefrColorScheme
import com.venom.wordvibe.core.theme.tokens.getDifficultyTheme

/**
 * Front face of the Word Mastery card.
 *
 * Displays:
 * - Header: CEFR badge (left), Difficulty-themed Semantic pill (center), Bookmark button (right)
 * - Hero word with syllable breakdown and rank indicator (dynamic color)
 * - Difficulty Meter Bar below the word
 * - Phonetic pronunciation with audio button
 * - Blurred example sentence as hint (auto-reveals on first tap)
 *
 * @param word Word data to display
 * @param isBookmarked Current bookmark state
 * @param isHintRevealed Whether the example hint is revealed
 * @param onSpeak Callback for TTS with text and rate
 * @param onBookmarkToggle Callback when bookmark is toggled
 * @param onRevealHint Callback when hint reveal button is tapped
 * @param modifier Modifier for styling
 */
@Composable
fun CardFront(
    word: WordMaster,
    isBookmarked: Boolean,
    isHintRevealed: Boolean,
    onSpeak: (text: String) -> Unit,
    onBookmarkToggle: () -> Unit,
    onRevealHint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cefrColors = getCefrColorScheme(word.cefrLevel)
    val difficultyTheme = getDifficultyTheme(word.difficultyScore)
    val hintExample by remember(word.cefrLevel, word.examples) {
        derivedStateOf {
            word.examples[word.cefrLevel] ?: word.examples.values.firstOrNull()
        }
    }

    // Resolve TTS once at card level
    val ttsViewModel: TTSViewModel = hiltViewModel(LocalContext.current as ViewModelStoreOwner)
    val ttsToggle: (String, String?) -> Unit = remember(ttsViewModel) {
        { text: String, lang: String? -> ttsViewModel.toggle(text, lang) }
    }
    val ttsSpeakSlow: (String, String?) -> Unit = remember(ttsViewModel) {
        { text: String, lang: String? -> ttsViewModel.speakSlow(text, lang) }
    }
    // Active-state for the example-sentence speaker: speaking THIS sentence only
    val ttsState by ttsViewModel.state.collectAsStateWithLifecycle()

    // Animated values
    val blurAmount by animateFloatAsState(
        targetValue = if (isHintRevealed) 0f else 6f,
        animationSpec = tween(durationMillis = 400)
    )

    val hintAlpha by animateFloatAsState(
        targetValue = if (isHintRevealed) 1f else 0.50f,
        animationSpec = tween(durationMillis = 400)
    )

    GradientGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .border(1.adp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(32.adp)),
        thickness = GlassThickness.UltraThick,
        gradientColors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        ),
        gradientAlpha = 0.05f,
        showBorder = false,
        showShadow = false,
        orbetColor = difficultyTheme.background,
        showOrbet = false,
        shape = RoundedCornerShape(32.adp),
        contentPadding = 20.adp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FlashcardHeader(
                word = word,
                cefrColors = cefrColors,
                difficultyTheme = difficultyTheme,
                isBookmarked = isBookmarked,
                onBookmarkToggle = onBookmarkToggle
            )

            // Hero word section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.adp, Alignment.CenterVertically),
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxWidth()
            ) {
                // Word with rank indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    DynamicStyledText(
                        text = word.wordEn.uppercase(),
                        maxFontSize = 42,
                        minFontSize = 34,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(end = 8.adp)
                    )

                    // Rank (Oxford seal removed - provenance unverified per ADR-0012)
                    Text(
                            text = "#${word.rank}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.asp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = difficultyTheme.text
                        )
                }

                // Syllables
                Text(
                    text = word.syllabify?.uppercase() ?: word.phoneticAr,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.asp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alpha(0.6f)
                )

                // Difficulty Meter
                DifficultyMeter(word.difficultyScore)

                Box(modifier = Modifier.padding(top = 8.adp)) {
                    PhoneticButton(
                        phonetic = word.phoneticUs,
                        wordEn = word.wordEn,
                        ttsToggle = ttsToggle,
                        ttsSpeakSlow = ttsSpeakSlow
                    )
                }
            }

            // Blurred hint example
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxWidth()
            ) {
                if (hintExample != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.8f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.adp)
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            InteractiveText(
                                text = hintExample.toString(),
                                onTtsToggle = ttsToggle,
                                onTtsSpeakSlow = ttsSpeakSlow,
                                onClick = onRevealHint,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .blur(blurAmount.adp)
                                    .alpha(hintAlpha)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "\"$hintExample\"",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontStyle = FontStyle.Italic,
                                            lineHeight = 26.asp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 4
                                    )
                                }
                            }
                        }
                        // Dedicated sentence-audio control (toggles this sentence only)
                        ExampleSpeakButton(
                            sentence = hintExample.toString(),
                            isSpeaking = ttsState.isSpeaking && ttsState.currentText == hintExample.toString(),
                            ttsToggle = ttsToggle,
                            ttsSpeakSlow = ttsSpeakSlow
                        )
                    }
                } else {
                    Box(modifier = Modifier.weight(0.8f))
                }

                // Tap indicator
                TapIndicator()
            }
        }
    }
}

/**
 * Phonetic button with IPA notation and speaker icon
 */
@Composable
private fun PhoneticButton(
    phonetic: String,
    wordEn: String,
    ttsToggle: (String, String?) -> Unit,
    ttsSpeakSlow: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    InteractiveText(
        wordEn,
        onTtsToggle = ttsToggle,
        onTtsSpeakSlow = ttsSpeakSlow,
        // transparent touch expansion: pill visual unchanged, tap area >= 48dp tall
        modifier = Modifier.heightIn(min = 48.dp)
    ) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .border(
                    1.adp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(50)
                )
                .padding(start = 20.adp, end = 8.adp, top = 8.adp, bottom = 8.adp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.adp)
        ) {
            Text(
                text = "/$phonetic/",
                style = MaterialTheme.typography.titleMedium.copy(
                    letterSpacing = 1.asp,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.85f)
            )
            // Divider
            Box(
                modifier = Modifier
                    .width(1.adp)
                    .height(16.adp)
                    .background(MaterialTheme.colorScheme.onBackground.copy(0.1f))
            )
            Box(
                modifier = Modifier
                    .size(40.adp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_speaker),
                    contentDescription = stringResource(R.string.mastery_speak),
                    modifier = Modifier.size(20.adp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Dedicated speaker control for the example sentence on the card front.
 *
 * Touch target is enforced at >= 48dp via a transparent expansion around the
 * compact visual circle. Long-press plays the sentence slowly (same contract as
 * every other voice target on the card). Active state (speaking this exact
 * sentence) tints the circle with the primary color.
 */
@Composable
private fun ExampleSpeakButton(
    sentence: String,
    isSpeaking: Boolean,
    ttsToggle: (String, String?) -> Unit,
    ttsSpeakSlow: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    InteractiveText(
        sentence,
        onTtsToggle = ttsToggle,
        onTtsSpeakSlow = ttsSpeakSlow,
        modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.adp)
                    .clip(CircleShape)
                    .background(
                        if (isSpeaking) {
                            MaterialTheme.colorScheme.primary.copy(0.2f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        }
                    )
                    .border(1.adp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_speaker),
                    contentDescription = stringResource(R.string.mastery_speak_example),
                    modifier = Modifier.size(20.adp),
                    tint = if (isSpeaking) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun CardFrontPreview() {
    WordVibeTheme(darkTheme = true) {
        CardFront(
            word = MockWordData.journeyWord,
            isBookmarked = false,
            isHintRevealed = false,
            onSpeak = { _ -> },
            onBookmarkToggle = {},
            onRevealHint = {},
            modifier = Modifier.size(320.adp, 580.adp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF1F5F9)
@Composable
private fun CardFrontPreviewLight() {
    WordVibeTheme(darkTheme = false) {
        CardFront(
            word = MockWordData.journeyWord,
            isBookmarked = false,
            isHintRevealed = false,
            onSpeak = { _ -> },
            onBookmarkToggle = {},
            onRevealHint = {},
            modifier = Modifier.size(320.adp, 580.adp)
        )
    }
}


