package com.venom.stackcard.ui.components.mastery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.venom.domain.model.AppTheme
import com.venom.domain.model.getFrequencyLabel
import com.venom.wordvibe.R
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp
import com.venom.wordvibe.ui.designsystem.WordVibeTheme
import com.venom.designsystem.theme.synapse
import com.venom.wordvibe.core.theme.tokens.getDifficultyTheme


@Composable
fun InteractiveText(
    text: String,
    modifier: Modifier = Modifier,
    languageTag: String? = null,
    onTtsToggle: (text: String, languageTag: String?) -> Unit = { _, _ -> },
    onTtsSpeakSlow: (text: String, languageTag: String?) -> Unit = { _, _ -> },
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val haptic = rememberHapticFeedback()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(150)
    )

    Box(
        modifier = modifier
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic(HapticStrength.LIGHT)
                    onTtsToggle(text, languageTag)
                    onClick()
                },
                onLongClick = {
                    haptic(HapticStrength.LIGHT)
                    onTtsSpeakSlow(text, languageTag)
                    onLongClick()
                }
            )
    ) {
        content()
    }
}

/**
 * Uppercase section header with optional leading icon.
 *
 * Used throughout the Insights sheet to label content sections.
 *
 * @param title The section title (will be displayed uppercase)
 * @param modifier Modifier for styling
 * @param icon Optional icon painter to display before the title
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null
) {
    Row(
        modifier = modifier.padding(bottom = 12.adp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.adp)
    ) {
        icon?.let {
            Icon(
                painter = it,
                contentDescription = null,
                modifier = Modifier.size(14.adp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.asp,
                fontSize = 11.asp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// BOOKMARK BUTTON COMPONENT

/**
 * Toggle button for bookmarking words with star icon.
 *
 * Displays filled star (yellow) when active, outlined star when inactive.
 * Includes scale animation on press.
 *
 * @param isBookmarked Current bookmark state
 * @param onToggle Callback when bookmark is toggled
 * @param modifier Modifier for styling
 */
@Composable
fun BookmarkButton(
    isBookmarked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()
    val backgroundColor by animateColorAsState(
        targetValue = if (isBookmarked) {
            MaterialTheme.colorScheme.tertiary.copy(0.1f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(200)
    )

    val borderColor by animateColorAsState(
        targetValue = if (isBookmarked) {
            MaterialTheme.colorScheme.tertiary.copy(0.3f)
        } else {
            MaterialTheme.colorScheme.outline.copy(0.5f)
        },
        animationSpec = tween(200)
    )

    val iconTint by animateColorAsState(
        targetValue = if (isBookmarked) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200)
    )

    IconButton(
        onClick = {
            haptic(HapticStrength.MEDIUM)
            onToggle()
        },
        modifier = modifier
            .size(40.adp)
            .background(backgroundColor, CircleShape)  // Apply shape here
            .border(1.adp, borderColor, CircleShape)
            .clip(CircleShape)  // Clip last to ensure everything is circular
    ) {
        Icon(
            painter = painterResource(
                id = if (isBookmarked) R.drawable.ic_star_fill else R.drawable.ic_star
            ),
            contentDescription = stringResource(
                id = if (isBookmarked) R.string.mastery_bookmark_remove else R.string.mastery_bookmark_add
            ),
            tint = iconTint,
            modifier = Modifier.size(24.adp)
        )
    }
}

/**
 * 5-bar horizontal visualization for word frequency.
 *
 * Displays frequency level (1-6) with filled/unfilled bars.
 * Level 1 = most common (5 bars), Level 6 = obscure (1 bar).
 *
 * @param frequency Frequency level from 1 to 6
 * @param modifier Modifier for styling
 */
@Composable
fun FrequencyWidget(
    frequency: Int,
    modifier: Modifier = Modifier
) {
    val label = getFrequencyLabel(frequency)

    // Map frequency to bars filled (inverse relationship)
    val barsToFill = when (frequency) {
        1 -> 5
        2 -> 5
        3 -> 4
        4 -> 3
        5 -> 2
        else -> 1
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.adp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                1.adp,
                MaterialTheme.colorScheme.outline.copy(0.18f),
                RoundedCornerShape(16.adp)
            )
            .padding(20.adp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.adp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chart_bar),
                    contentDescription = null,
                    modifier = Modifier.size(14.adp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.mastery_frequency).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.asp,
                        fontSize = 11.asp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(12.adp))

        // Bar visualization
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.adp)
        ) {
            repeat(5) { index ->
                val isFilled = index < barsToFill

                val barColor by animateColorAsState(
                    targetValue = if (isFilled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    animationSpec = tween(300, delayMillis = index * 50),
                    label = "barColor$index"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.adp)
                        .clip(RoundedCornerShape(4.adp))
                        .background(barColor)
                )
            }
        }
    }
}


/**
 * Reusable stat cell for the 2x2 grid.
 */
@Composable
fun StatCell(
    label: String,
    watermarkIcon: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1.8f)
            .clip(RoundedCornerShape(16.adp))
            .shadow(
                elevation = 4.adp,
                shape = RoundedCornerShape(16.adp),
                spotColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.15f)
            )
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                0.5.adp,
                MaterialTheme.colorScheme.outline.copy(0.18f),
                RoundedCornerShape(16.adp)
            )
            .padding(14.adp)
    ) {
        // Watermark icon - large background
        Icon(
            painter = painterResource(id = watermarkIcon),
            contentDescription = null,
            modifier = Modifier
                .size(32.adp)
                .align(Alignment.TopEnd)
                .alpha(0.1f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.asp,
                    fontSize = 10.asp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.80f)
            )
            content()
        }
    }
}

/**
 * Returns a human-readable frequency band label and sublabel based on word rank.
 * Rank is 1-indexed (rank=1 is the most common word in the database).
 */
private fun rankBand(rank: Int): Pair<String, String> = when {
    rank <= 0    -> "Power Word" to "Essential vocabulary"
    rank <= 1000 -> "Top 1000" to "Most essential words"
    rank <= 2000 -> "Top 2000" to "Essential words"
    rank <= 3000 -> "Top 3000" to "High-frequency words"
    rank <= 5000 -> "Top 5000" to "Common vocabulary"
    else         -> "Extended" to "Advanced vocabulary"
}

@Composable
fun PowerCell(
    showPowerTip: Boolean,
    onTogglePowerTip: () -> Unit,
    modifier: Modifier = Modifier,
    wordRank: Int = 0
) {
    val semanticColors = MaterialTheme.synapse.semantic
    val (bandLabel, bandSub) = remember(wordRank) { rankBand(wordRank) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.adp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .aspectRatio(1.8f)
            .border(
                width = 1.adp,
                color = if (showPowerTip) {
                    semanticColors.gold.copy(0.3f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(0.2f)
                },
                shape = RoundedCornerShape(16.adp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTogglePowerTip
            )
    ) {
        AnimatedContent(
            targetState = showPowerTip,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith
                        fadeOut(animationSpec = tween(200))
            }
        ) { isPowerTipVisible ->
            if (isPowerTipVisible) {
                Box(
                    modifier = modifier
                        .aspectRatio(1.8f)
                        .background(semanticColors.goldContainer)
                        .padding(12.adp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.mastery_power_tip_message),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 16.asp
                        ),
                        textAlign = TextAlign.Center,
                        color = semanticColors.onGold
                    )
                }
            } else {
                Column(modifier = modifier) {
                    StatCell(
                        label = stringResource(R.string.mastery_power),
                        watermarkIcon = R.drawable.ic_lightning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.adp)
                        ) {
                            Text(
                                text = "#$wordRank",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Column {
                                Text(
                                    text = bandLabel,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = bandSub,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TapIndicator(
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.adp),
        modifier = modifier.alpha(0.3f)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_hand_tap),
            contentDescription = null,
            modifier = Modifier.size(18.adp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.mastery_tap_to_flip).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.asp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun PowerCellFlippedPreview() {
    WordVibeTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(16.adp)) {
            PowerCell(
                showPowerTip = true,
                wordRank = 1450,
                onTogglePowerTip = {},
                modifier = Modifier.aspectRatio(1.8f)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF1F5F9)
@Composable
private fun PowerCellPreviewLight() {
    WordVibeTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(16.adp)) {
            PowerCell(
                showPowerTip = false,
                wordRank = 850,
                onTogglePowerTip = {},
                modifier = Modifier.aspectRatio(1.8f)
            )
        }
    }
}

@Composable
fun DifficultyMeter(
    difficultyScore: Int,
    modifier: Modifier = Modifier
) {
    val theme = getDifficultyTheme(difficultyScore)
    val progress = (difficultyScore.coerceIn(0, 10) / 10f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .width(64.adp)
            .height(3.adp)
            .clip(RoundedCornerShape(50))
            .background(theme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = progress)
                .clip(RoundedCornerShape(50))
                .background(theme.text)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun SectionHeaderPreview() {
    WordVibeTheme {
        SectionHeader(
            title = "Synonyms",
            icon = painterResource(id = R.drawable.ic_equals),
            modifier = Modifier.padding(16.adp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun BookmarkButtonPreview() {
    WordVibeTheme {
        Row(
            modifier = Modifier.padding(16.adp),
            horizontalArrangement = Arrangement.spacedBy(16.adp)
        ) {
            BookmarkButton(isBookmarked = false, onToggle = {})
            BookmarkButton(isBookmarked = true, onToggle = {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun FrequencyWidgetPreview() {
    WordVibeTheme {
        Column(
            modifier = Modifier.padding(16.adp),
            verticalArrangement = Arrangement.spacedBy(16.adp)
        ) {
            FrequencyWidget(frequency = 1)
            FrequencyWidget(frequency = 3)
            FrequencyWidget(frequency = 5)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun InteractiveTextPreview() {
    WordVibeTheme {
        InteractiveText(
            text = "Hello World",
            onTtsToggle = { _, _ -> },
            onTtsSpeakSlow = { _, _ -> },
            modifier = Modifier.padding(16.adp)
        ) {
            Text(
                text = "Tap or long-press me to speak",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}



