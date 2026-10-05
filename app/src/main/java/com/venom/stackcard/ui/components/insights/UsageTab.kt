package com.venom.stackcard.ui.components.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.venom.data.mock.MockWordData
import com.venom.domain.model.AppTheme
import com.venom.domain.model.CefrLevel
import com.venom.domain.model.WordMaster
import com.venom.wordvibe.R
import com.venom.stackcard.ui.components.mastery.InteractiveText
import com.venom.stackcard.ui.components.mastery.SectionHeader
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp
import com.venom.wordvibe.ui.designsystem.WordVibeTheme

import com.venom.wordvibe.core.theme.tokens.getCefrColorScheme

/**
 * Usage tab content for Insights sheet.
 *
 * @param word The word to display usage information for
 * @param modifier Modifier for styling
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UsageTab(
    word: WordMaster,
    modifier: Modifier = Modifier,
    userCefrLevel: CefrLevel
) {
    val scrollState = rememberScrollState()

    // Derived boolean checks
    val hasUsageNote by remember(word.usageNote) {
        derivedStateOf { !word.usageNote.isNullOrBlank() }
    }

    val hasCollocations by remember(word.collocations) {
        derivedStateOf { word.collocations.isNotEmpty() }
    }

    val hasExamples by remember(word.examples) {
        derivedStateOf { word.examples.isNotEmpty() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.adp)
            .padding(bottom = 40.adp),
        verticalArrangement = Arrangement.spacedBy(24.adp)
    ) {
        // Teacher's Tip
        if (hasUsageNote) {
            TeachersTipCard(usageNote = word.usageNote!!)
        }

        // Collocations
        if (hasCollocations) {
            CollocationsSection(
                collocations = word.collocations,
            )
        }

        // Contextual Examples - Smart highlighting + 0.3s animations
        if (hasExamples) {
            ContextualExamplesSection(
                examples = word.examples,
                targetWord = word.wordEn,
                userCefrLevel = userCefrLevel
            )
        }
    }
}

/**
 * Teacher's Tip card
 */
@Composable
private fun TeachersTipCard(
    usageNote: String,
    modifier: Modifier = Modifier
) {
    val accentColor = MaterialTheme.colorScheme.tertiary
    val bgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
    val borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.adp))
            .background(bgColor)
            .border(
                1.adp,
                borderColor,
                RoundedCornerShape(20.adp)
            )
            .padding(20.adp)
    ) {
        // Watermark lightbulb
        Icon(
            painter = painterResource(id = R.drawable.ic_lightbulb),
            contentDescription = null,
            modifier = Modifier
                .size(120.adp)
                .align(Alignment.TopEnd)
                .offset(x = 16.adp, y = (-16).adp)
                .alpha(0.06f)
                .rotate(12f),
            tint = accentColor
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.adp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.adp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.adp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_student),
                        contentDescription = null,
                        modifier = Modifier.size(16.adp),
                        tint = accentColor
                    )
                }
                Text(
                    text = stringResource(R.string.mastery_teachers_tip).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.asp
                    ),
                    color = accentColor
                )
            }

            // Usage note text
            Text(
                text = "\"$usageNote\"",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    lineHeight = 24.asp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(0.9f)
            )
        }
    }
}

/**
 * Collocations section
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CollocationsSection(
    collocations: List<String>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val borderColor = primaryColor.copy(alpha = 0.3f)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.adp)
    ) {
        SectionHeader(
            title = stringResource(R.string.mastery_collocations),
            icon = painterResource(id = R.drawable.ic_link)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.adp),
            verticalArrangement = Arrangement.spacedBy(8.adp)
        ) {
            collocations.forEach { collocation ->
                CollocationChip(
                    text = collocation,
                    textColor = primaryColor,
                    borderColor = borderColor
                )
            }
        }
    }
}

/**
 * Collocation chip - Outlined style with theme colors
 */
@Composable
private fun CollocationChip(
    text: String,
    textColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    InteractiveText(text) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.adp))
                .border(
                    width = 1.5.adp,
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
 * Contextual examples section with CEFR-level accordions
 */
@Composable
private fun ContextualExamplesSection(
    examples: Map<CefrLevel, String>,
    targetWord: String,
    userCefrLevel: CefrLevel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.adp)
    ) {
        SectionHeader(
            title = stringResource(R.string.mastery_contextual_examples),
            icon = painterResource(id = R.drawable.ic_quotes)
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.adp)) {
            CefrLevel.entries.forEach { level ->
                val example = examples[level]
                if (example != null) {
                    ExampleAccordionItem(
                        level = level,
                        example = example,
                        targetWord = targetWord,
                        isUserLevel = level == userCefrLevel
                    )
                }
            }
        }
    }
}

/**
 * Accordion item with smart word highlighting and 0.3s animations
 */
@Composable
private fun ExampleAccordionItem(
    level: CefrLevel,
    example: String,
    targetWord: String,
    isUserLevel: Boolean,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember(level, isUserLevel) {
        mutableStateOf(isUserLevel) // Auto-expand only user's level
    }

    val cefrColors = getCefrColorScheme(level)

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "chevron_rotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.adp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                width = if (isUserLevel) 1.adp else 0.5.adp,
                color = if (isUserLevel) {
                    MaterialTheme.colorScheme.primary.copy(0.3f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(0.2f)
                },
                shape = RoundedCornerShape(16.adp)
            )
    ) {
        // Header (always visible)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { isExpanded = !isExpanded }
                .padding(14.adp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.adp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // CEFR badge - color-coded
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.adp))
                        .background(cefrColors.background)
                        .border(
                            0.5.adp,
                            cefrColors.border,
                            RoundedCornerShape(6.adp)
                        )
                        .padding(horizontal = 8.adp, vertical = 4.adp)
                ) {
                    Text(
                        text = level.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.asp
                        ),
                        color = cefrColors.content
                    )
                }

                // User level indicator
                if (isUserLevel) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.adp))
                            .background(MaterialTheme.colorScheme.primary.copy(0.15f))
                            .padding(horizontal = 6.adp, vertical = 2.adp)
                    ) {
                        Text(
                            text = "YOUR LEVEL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.asp,
                                letterSpacing = 0.8.asp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Chevron with smooth rotation
            Icon(
                painter = painterResource(id = R.drawable.ic_caret_down),
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier
                    .size(16.adp)
                    .rotate(rotationAngle),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
            )
        }

        // Content with enhanced 0.3s animation
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(300)
            ),
            exit = shrinkVertically(
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(
                animationSpec = tween(300)
            )
        ) {
            InteractiveText(example) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.adp, end = 14.adp, bottom = 14.adp)
                ) {
                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.adp)
                            .background(MaterialTheme.colorScheme.outline.copy(0.1f))
                            .size(height = 1.adp, width = 1.adp)
                    )

                    // Smart highlighted text
                    HighlightedExampleText(
                        example = example,
                        targetWord = targetWord
                    )
                }
            }
        }
    }
}

/**
 * Text with smart word highlighting
 */
@Composable
private fun HighlightedExampleText(
    example: String,
    targetWord: String,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val annotatedString = remember(example, targetWord) {
        buildAnnotatedString {
            if (targetWord.isBlank()) {
                append(example)
                return@buildAnnotatedString
            }

            val lowerExample = example.lowercase()
            val lowerTarget = targetWord.lowercase()

            var lastIndex = 0      // tracks how far we've appended
            var searchFrom = 0     // tracks where to search next (independent!)
            var currentIndex = lowerExample.indexOf(lowerTarget, searchFrom)

            while (currentIndex != -1) {
                val endIndex = currentIndex + lowerTarget.length

                // Check word boundaries
                val charBefore = if (currentIndex > 0) lowerExample[currentIndex - 1] else ' '
                val charAfter = if (endIndex < lowerExample.length) lowerExample[endIndex] else ' '
                val isWordStart = charBefore == ' ' || !charBefore.isLetterOrDigit()
                val isWordEnd = charAfter == ' ' || !charAfter.isLetterOrDigit()

                if (isWordStart && isWordEnd) {
                    // Append all plain text from lastIndex up to this match
                    append(example.substring(lastIndex, currentIndex))

                    // Append highlighted match (preserve original casing)
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryColor
                        )
                    ) {
                        append(example.substring(currentIndex, endIndex))
                    }

                    lastIndex = endIndex
                    searchFrom = endIndex
                } else {
                    // NOT a whole-word match — skip but do NOT touch lastIndex
                    searchFrom = currentIndex + 1
                }

                currentIndex = lowerExample.indexOf(lowerTarget, searchFrom)
            }

            // Append any remaining text after the last match
            if (lastIndex < example.length) {
                append(example.substring(lastIndex))
            }
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(
            lineHeight = 22.asp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun UsageTabPreview() {
    WordVibeTheme(darkTheme = true) {
        UsageTab(
            word = MockWordData.journeyWord,
            userCefrLevel = CefrLevel.A2
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF1F5F9)
@Composable
private fun UsageTabPreviewLight() {
    WordVibeTheme(darkTheme = false) {
        UsageTab(
            word = MockWordData.journeyWord,
            userCefrLevel = CefrLevel.C2
        )
    }
}


