package com.venom.stackcard.ui.components.insights

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.venom.data.mock.MockWordData
import com.venom.domain.model.AppTheme
import com.venom.domain.model.LanguageOption
import com.venom.domain.model.WordMaster
import com.venom.domain.model.getLanguageOptions
import com.venom.wordvibe.R
import com.venom.stackcard.ui.components.mastery.InteractiveText
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp
import com.venom.wordvibe.ui.designsystem.WordVibeTheme

/**
 * Languages tab content for Insights sheet.
 *
 * Displays a list of available language translations with:
 * - Dynamic sorting (pinned languages at top)
 *
 * @param word The word to get translations from
 * @param pinnedLanguage Currently pinned language (if any)
 * @param onPinLanguage Callback when a language is pinned/unpinned
 * @param modifier Modifier for styling
 */
@Composable
fun LanguagesTab(
    word: WordMaster,
    pinnedLanguage: LanguageOption?,
    onPinLanguage: (LanguageOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Get all available languages and sort with pinned first
    val languages by remember(word, pinnedLanguage) {
        derivedStateOf {
            val allLanguages = getLanguageOptions(word)
            allLanguages.sortedByDescending { language ->
                pinnedLanguage?.langName == language.langName
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.adp, vertical = 24.adp)
            .padding(bottom = 40.adp),
        verticalArrangement = Arrangement.spacedBy(12.adp)
    ) {
        // Info text
        Text(
            text = stringResource(R.string.mastery_pin_language_hint),
            style = MaterialTheme.typography.bodySmall.copy(
                lineHeight = 20.asp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
        )

        // Language list (sorted with pinned at top)
        languages.forEach { language ->
            if (language.translation != null) {
                LanguageRow(
                    language = language,
                    isPinned = pinnedLanguage?.langName == language.langName,
                    onPin = { onPinLanguage(language) }
                )
            }
        }
    }
}

/**
 * Language row with pinned state and glow effect
 */
@Composable
private fun LanguageRow(
    language: LanguageOption,
    isPinned: Boolean,
    onPin: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animated colors
    val backgroundColor by animateColorAsState(
        targetValue = if (isPinned) {
            MaterialTheme.colorScheme.primary.copy(0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(250)
    )

    val borderColor by animateColorAsState(
        targetValue = if (isPinned) {
            MaterialTheme.colorScheme.primary.copy(0.4f)
        } else {
            MaterialTheme.colorScheme.outline.copy(0.2f)
        },
        animationSpec = tween(250)
    )

    val pinButtonBg by animateColorAsState(
        targetValue = if (isPinned) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(250)
    )

    val pinIconTint by animateColorAsState(
        targetValue = if (isPinned) {
            Color.White
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(250)
    )

    val pinButtonScale by animateFloatAsState(
        targetValue = if (isPinned) 1.05f else 1f,
        animationSpec = tween(250)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.adp))
            .background(backgroundColor)
            .border(
                width = if (isPinned) 1.5.adp else 0.5.adp,
                color = borderColor,
                shape = RoundedCornerShape(16.adp)
            )
            .padding(16.adp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Language content
        InteractiveText(
            text = language.translation ?: "",
            modifier = Modifier.weight(1f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.adp)) {
                // Language name
                Text(
                    text = language.langName.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.asp,
                        fontSize = 9.asp
                    ),
                    color = if (isPinned) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                    }
                )

                // Translation
                Text(
                    text = language.translation ?: "",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Pin button with scale animation
        Box(
            modifier = Modifier
                .size(44.adp)
                .scale(pinButtonScale)
                .clip(CircleShape)
                .background(pinButtonBg)
                .border(
                    width = if (isPinned) 1.5.adp else 0.5.adp,
                    color = if (isPinned) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(0.2f)
                    },
                    shape = CircleShape
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onPin
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    id = if (isPinned) R.drawable.ic_push_pin_fill else R.drawable.ic_push_pin
                ),
                contentDescription = stringResource(
                    if (isPinned) R.string.mastery_unpin else R.string.mastery_pin
                ),
                modifier = Modifier.size(18.adp),
                tint = pinIconTint
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun LanguagesTabPreview() {
    WordVibeTheme(darkTheme = true) {
        LanguagesTab(
            word = MockWordData.journeyWord,
            pinnedLanguage = LanguageOption("German", "Reise"),
            onPinLanguage = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF1F5F9)
@Composable
private fun LanguagesTabPreviewLight() {
    WordVibeTheme(darkTheme = false) {
        LanguagesTab(
            word = MockWordData.journeyWord,
            pinnedLanguage = LanguageOption("French", "voyage"),
            onPinLanguage = {}
        )
    }
}

@Preview(showBackground = true, name = "No Pinned Language")
@Composable
private fun LanguagesTabNoPinPreview() {
    WordVibeTheme(darkTheme = true) {
        LanguagesTab(
            word = MockWordData.journeyWord,
            pinnedLanguage = null,
            onPinLanguage = {}
        )
    }
}


