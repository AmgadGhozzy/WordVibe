package com.venom.stackcard.ui.components.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import com.venom.data.mock.MockWordData
import com.venom.domain.model.AppTheme
import com.venom.domain.model.WordMaster
import com.venom.wordvibe.R
import com.venom.stackcard.ui.components.mastery.InteractiveText
import com.venom.stackcard.ui.components.mastery.SectionHeader
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp
import com.venom.wordvibe.ui.designsystem.WordVibeTheme
import com.venom.designsystem.theme.synapse
import com.venom.wordvibe.core.theme.tokens.getPosColor

/**
 * Relations tab content for Insights sheet.
 *
 * @param word The word to display relations for
 * @param modifier Modifier for styling
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RelationsTab(
    word: WordMaster,
    modifier: Modifier = Modifier
) {
    // Cached boolean checks
    val hasSynonyms by remember(word.synonyms) {
        derivedStateOf { word.synonyms.isNotEmpty() }
    }
    val hasAntonyms by remember(word.antonyms) {
        derivedStateOf { word.antonyms.isNotEmpty() }
    }
    val hasRelatedWords by remember(word.relatedWords.english) {
        derivedStateOf { word.relatedWords.english.isNotEmpty() }
    }

    // Pre-compute word family entries
    val wordFamilyEntries = remember(word.wordFamily) {
        listOfNotNull(
            word.wordFamily.noun?.let { "noun" to it },
            word.wordFamily.verb?.let { "verb" to it },
            word.wordFamily.adj?.let { "adj" to it },
            word.wordFamily.adv?.let { "adv" to it }
        )
    }

    val hasWordFamily by remember(wordFamilyEntries) {
        derivedStateOf { wordFamilyEntries.isNotEmpty() }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.adp, vertical = 24.adp)
            .padding(bottom = 40.adp),
        verticalArrangement = Arrangement.spacedBy(24.adp)
    ) {
        // Synonyms
        if (hasSynonyms) {
            SynonymsSection(word.synonyms)
        }

        // Antonyms
        if (hasAntonyms) {
            AntonymsSection(word.antonyms)
        }

        // Related concepts - EN prioritized, AR at 60% opacity
        if (hasRelatedWords) {
            RelatedConceptsSection(
                englishWords = word.relatedWords.english,
                arabicWords = word.relatedWords.arabic
            )
        }

        // Word family
        if (hasWordFamily) {
            WordFamilySection(
                entries = wordFamilyEntries
            )
        }
    }
}

/**
 * Synonyms section
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SynonymsSection(
    synonyms: List<String>,
    modifier: Modifier = Modifier
) {
    val synonymColors = MaterialTheme.synapse.semantic

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.adp)
    ) {
        SectionHeader(
            title = stringResource(R.string.mastery_synonyms),
            icon = painterResource(id = R.drawable.ic_equals)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.adp),
            verticalArrangement = Arrangement.spacedBy(8.adp)
        ) {
            synonyms.forEach { synonym ->
                RelationChip(
                    text = synonym,
                    backgroundColor = synonymColors.successContainer,
                    borderColor = synonymColors.successBorder,
                    textColor = synonymColors.success
                )
            }
        }
    }
}

/**
 * Antonyms section
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AntonymsSection(
    antonyms: List<String>,
    modifier: Modifier = Modifier
) {
    val antonymColors = MaterialTheme.synapse.semantic

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.adp)
    ) {
        SectionHeader(
            title = stringResource(R.string.mastery_antonyms),
            icon = painterResource(id = R.drawable.ic_arrows_left_right)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.adp),
            verticalArrangement = Arrangement.spacedBy(8.adp)
        ) {
            antonyms.forEach { antonym ->
                RelationChip(
                    text = antonym,
                    backgroundColor = antonymColors.errorContainer,
                    borderColor = antonymColors.errorBorder,
                    textColor = antonymColors.error
                )
            }
        }
    }
}

/**
 * Related concepts - English prioritized, Arabic at 60% opacity
 */
@Composable
private fun RelatedConceptsSection(
    englishWords: List<String>,
    arabicWords: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.adp)
    ) {
        SectionHeader(
            title = stringResource(R.string.mastery_related_concepts),
            icon = painterResource(id = R.drawable.ic_circles_three_plus)
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.adp)) {
            englishWords.forEachIndexed { index, englishWord ->
                val arabicWord = arabicWords.getOrNull(index)

                RelatedConceptItem(
                    englishWord = englishWord,
                    arabicWord = arabicWord,
                    modifier = Modifier
                )
            }
        }
    }
}

/**
 * Word family - Uses getPosColor() semantic colors
 */
@Composable
private fun WordFamilySection(
    entries: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.adp)
    ) {
        SectionHeader(
            title = stringResource(R.string.mastery_word_family),
            icon = painterResource(id = R.drawable.ic_tree_structure)
        )

        // 2-column grid
        val columns = remember(entries) { entries.chunked(2) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.adp)
        ) {
            columns.forEach { columnItems ->
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.adp)
                ) {
                    columnItems.forEach { (pos, term) ->
                        if (term.isNotBlank()) {
                            WordFamilyCard(
                                partOfSpeech = pos,
                                term = term
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable relation chip with theme colors
 */
@Composable
private fun RelationChip(
    text: String,
    backgroundColor: Color,
    borderColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    InteractiveText(
        text = text
    ) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.adp))
                .background(backgroundColor)
                .border(
                    width = 1.adp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.adp)
                )
                .padding(horizontal = 14.adp, vertical = 10.adp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = textColor
            )
        }
    }
}

/**
 * Related concept item - Arabic at 60% opacity for visual hierarchy
 */
@Composable
private fun RelatedConceptItem(
    englishWord: String,
    arabicWord: String?,
    modifier: Modifier = Modifier
) {
    InteractiveText(
        text = englishWord
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.adp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(
                    width = 0.5.adp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.adp)
                )
                .padding(14.adp),
            horizontalArrangement = Arrangement.spacedBy(10.adp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // English word (primary)
            Text(
                text = englishWord,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (arabicWord != null) {
                // Separator dot
                Box(
                    modifier = Modifier
                        .size(3.adp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                )

                // Arabic text with RTL layout - 60% opacity for hierarchy
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = arabicWord,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * Small word family card with semantic POS badge colors from getPosColor()
 */
@Composable
private fun WordFamilyCard(
    partOfSpeech: String,
    term: String,
    modifier: Modifier = Modifier
) {
    // Get semantic color based on POS type
    val posColor = getPosColor(partOfSpeech)

    InteractiveText(
        text = term
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.adp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(
                    width = 0.5.adp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.adp)
                )
                .padding(12.adp),
            verticalArrangement = Arrangement.spacedBy(6.adp)
        ) {
            // POS badge with semantic color
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.adp))
                    .background(posColor.copy(alpha = 0.15f))
                    .border(
                        0.5.adp,
                        posColor.copy(alpha = 0.3f),
                        RoundedCornerShape(6.adp)
                    )
                    .padding(horizontal = 8.adp, vertical = 3.adp)
            ) {
                Text(
                    text = when (partOfSpeech.lowercase()) {
                        "noun" -> stringResource(R.string.pos_noun).uppercase()
                        "verb" -> stringResource(R.string.pos_verb).uppercase()
                        "adj", "adjective" -> stringResource(R.string.pos_adj).uppercase()
                        "adv", "adverb" -> stringResource(R.string.pos_adv).uppercase()
                        else -> partOfSpeech.uppercase()
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.asp,
                        letterSpacing = 0.8.asp
                    ),
                    color = posColor
                )
            }

            // Word term
            Text(
                text = term,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun RelationsTabPreview() {
    WordVibeTheme(darkTheme = true) {
        RelationsTab(
            word = MockWordData.journeyWord
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF1F5F9)
@Composable
private fun RelationsTabPreviewLight() {
    WordVibeTheme(darkTheme = false) {
        RelationsTab(
            word = MockWordData.journeyWord
        )
    }
}


