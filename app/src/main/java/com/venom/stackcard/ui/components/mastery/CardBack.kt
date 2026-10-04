package com.venom.stackcard.ui.components.mastery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import com.venom.data.mock.MockWordData
import com.venom.domain.model.AppTheme
import com.venom.domain.model.LanguageOption
import com.venom.domain.model.WordMaster
import com.venom.ui.components.common.DynamicStyledText
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp
import com.venom.ui.components.other.GlassThickness
import com.venom.ui.components.other.GradientGlassCard
import com.venom.ui.viewmodel.TTSViewModel
import com.venom.wordvibe.R
import com.venom.designsystem.theme.SynapseTheme
import com.venom.wordvibe.core.theme.tokens.getCefrColorScheme
import com.venom.wordvibe.core.theme.tokens.getDifficultyTheme

/**
 * Back face of the Word Mastery card.
 *
 * Displays Arabic translation and meaning:
 * - Hero Arabic word with phonetic and transliteration
 * - "Memory Hook" Sticky Note (Randomized)
 * - Definition block (English + Arabic)
 * - Pinned language translation (if set)
 *
 * @param word Word data to display
 * @param isBookmarked Current bookmark state
 * @param pinnedLanguage Optional pinned language to display
 * @param onSpeak Callback for TTS with text and rate
 * @param onBookmarkToggle Callback when bookmark is toggled
 * @param modifier Modifier for styling
 */
@Composable
fun CardBack(
    word: WordMaster,
    isBookmarked: Boolean,
    pinnedLanguage: LanguageOption?,
    onSpeak: (text: String) -> Unit,
    onBookmarkToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cefrColors = getCefrColorScheme(word.cefrLevel)
    val difficultyTheme = getDifficultyTheme(word.difficultyScore)
    val scrollState = rememberScrollState()

    // Resolve TTS once at card level
    val ttsViewModel: TTSViewModel = hiltViewModel(LocalContext.current as ViewModelStoreOwner)
    val ttsToggle: (String, String?) -> Unit = remember(ttsViewModel) {
        { text: String, lang: String? -> ttsViewModel.toggle(text, lang) }
    }
    val ttsSpeakSlow: (String, String?) -> Unit = remember(ttsViewModel) {
        { text: String, lang: String? -> ttsViewModel.speakSlow(text, lang) }
    }

    // Stable lambda for speaking Arabic
    val speakArabic = remember(word.arabicAr, onSpeak) {
        { onSpeak(word.arabicAr) }
    }

    GradientGlassCard(
        modifier = modifier.fillMaxWidth(),
        thickness = GlassThickness.UltraThick,
        gradientColors = listOf(
            MaterialTheme.colorScheme.primary,
            difficultyTheme.background
        ),
        gradientAlpha = 0.12f,
        showBorder = true,
        showShadow = true,
        orbetColor = difficultyTheme.background,
        showOrbet = true,
        shape = RoundedCornerShape(32.adp),
        contentPadding = 20.adp
    ) {
        FlashcardHeader(
            word = word,
            cefrColors = cefrColors,
            difficultyTheme = difficultyTheme,
            isBookmarked = isBookmarked,
            onBookmarkToggle = onBookmarkToggle
        )

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(top = 20.adp, bottom = 32.adp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.adp)
        ) {
            // Primary arabic word
            DynamicStyledText(
                text = word.arabicAr,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                maxFontSize = 40,
                minFontSize = 30,
                lineHeight = 45,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Phonetic Arabic
            Text(
                text = word.phoneticAr,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 30.asp,
                    fontFamily = MaterialTheme.typography.bodyLarge.fontFamily
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Transliteration with speaker
            TransliterationRow(
                transliteration = word.translit,
                ttsToggle = ttsToggle,
                ttsSpeakSlow = ttsSpeakSlow
            )

            // Memory hook (sticky note)
            word.mnemonicAr?.let { mnemonic ->
                Box(modifier = Modifier.padding(top = 4.adp)) {
                    StickyNoteCard(
                        mnemonicText = mnemonic,
                        wordId = word.id,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Definition block
            Box(modifier = Modifier.padding(top = 16.adp)) {
                DefinitionBlock(
                    definitionEn = word.definitionEn,
                    definitionAr = word.definitionAr
                )
            }

            // Context Sentence (Example)
            val contextSentence = remember(word.cefrLevel, word.examples) {
                word.examples[word.cefrLevel] ?: word.examples.values.firstOrNull()
            }
            contextSentence?.let { sentence ->
                Box(modifier = Modifier.padding(top = 16.adp)) {
                    ContextSentenceBlock(sentence)
                }
            }

            // Pinned language
            pinnedLanguage?.translation?.let { translation ->
                Box(modifier = Modifier.padding(top = 16.adp)) {
                    PinnedLanguageCard(
                        language = pinnedLanguage,
                        translation = translation,
                        ttsToggle = ttsToggle,
                        ttsSpeakSlow = ttsSpeakSlow
                    )
                }
            }

            // Bottom handle visual cue
            Box(
                modifier = Modifier
                    .padding(top = 32.adp)
                    .width(40.adp)
                    .height(4.adp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun ContextSentenceBlock(
    sentence: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.adp))
            .background(MaterialTheme.colorScheme.primary.copy(0.08f))
            .border(
                1.adp,
                MaterialTheme.colorScheme.primary.copy(0.15f),
                RoundedCornerShape(20.adp)
            )
            .padding(20.adp),
        verticalArrangement = Arrangement.spacedBy(8.adp)
    ) {
        Text(
            text = stringResource(com.venom.wordvibe.R.string.context_label),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.asp
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "\"$sentence\"",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontStyle = FontStyle.Italic,
                lineHeight = 24.asp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Extracted transliteration row for better composition
 */
@Composable
private fun TransliterationRow(
    transliteration: String,
    ttsToggle: (String, String?) -> Unit,
    ttsSpeakSlow: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    InteractiveText(
        transliteration,
        onTtsToggle = ttsToggle,
        onTtsSpeakSlow = ttsSpeakSlow
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.adp),
            modifier = modifier
        ) {
            Text(
                text = transliteration,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 1.asp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier
                    .size(32.adp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_speaker),
                    contentDescription = stringResource(R.string.mastery_speak),
                    modifier = Modifier.size(16.adp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Extracted definition block for better composition and reusability
 */
@Composable
private fun DefinitionBlock(
    definitionEn: String,
    definitionAr: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.adp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(0.45f))
            .border(
                1.adp,
                MaterialTheme.colorScheme.outline.copy(0.25f),
                RoundedCornerShape(20.adp)
            )
            .padding(20.adp),
        verticalArrangement = Arrangement.spacedBy(12.adp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.adp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_book_open),
                contentDescription = null,
                modifier = Modifier.size(14.adp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.mastery_meaning).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.asp,
                    fontSize = 10.asp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // English definition
        DynamicStyledText(
            text = definitionEn,
            maxFontSize = 24,
            minFontSize = 18,
            lineHeight = 20,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Arabic definition (if exists)
        definitionAr?.let { arabicDef ->
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.adp),
                color = MaterialTheme.colorScheme.outline.copy(0.2f)
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                DynamicStyledText(
                    text = arabicDef,
                    maxFontSize = 24,
                    minFontSize = 18,
                    lineHeight = 24,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Pinned language card
 */
@Composable
private fun PinnedLanguageCard(
    language: LanguageOption,
    translation: String,
    ttsToggle: (String, String?) -> Unit,
    ttsSpeakSlow: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {

    InteractiveText(
        translation,
        onTtsToggle = ttsToggle,
        onTtsSpeakSlow = ttsSpeakSlow
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.adp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(0.45f))
                .border(
                    1.adp,
                    MaterialTheme.colorScheme.outline.copy(0.25f),
                    RoundedCornerShape(16.adp)
                )
                .padding(16.adp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.adp)
            ) {
                Text(
                    text = language.langName.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.asp,
                        fontSize = 9.asp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = translation,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Icon(
                painter = painterResource(id = R.drawable.ic_push_pin_fill),
                contentDescription = null,
                modifier = Modifier.size(18.adp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun CardBackNoPinPreview() {
    SynapseTheme(darkTheme = true) {
        CardBack(
            word = MockWordData.journeyWord,
            isBookmarked = true,
            pinnedLanguage = null,
            onSpeak = { _ -> },
            onBookmarkToggle = {},
            modifier = Modifier.size(320.adp, 600.adp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF1F5F9)
@Composable
private fun CardBackNoPinPreviewLight() {
    SynapseTheme(darkTheme = false) {
        CardBack(
            word = MockWordData.journeyWord,
            isBookmarked = true,
            pinnedLanguage = null,
            onSpeak = { _ -> },
            onBookmarkToggle = {},
            modifier = Modifier.size(320.adp, 600.adp)
        )
    }
}

