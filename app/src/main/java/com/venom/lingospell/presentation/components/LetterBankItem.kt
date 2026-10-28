package com.venom.lingospell.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.venom.domain.model.AppTheme
import com.venom.lingospell.domain.Letter
import com.venom.lingospell.domain.LetterStatus
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp
import com.venom.wordvibe.ui.designsystem.WordVibeTheme
import com.venom.designsystem.theme.synapse

/**
 * Single letter tile in the letter bank.
 *
 * Color Usage (Semantic):
 * - Background: surfaceTile
 * - Text: textSecondary
 * - Border: outline (subtle)
 * - Shadow: shadowPrimary
 * - Used state placeholder: outlineVariant
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LetterBankItem(
    letter: Letter,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = modifier.aspectRatio(1f / 1.1f),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = letter.status == LetterStatus.AVAILABLE,
            enter = fadeIn() + scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                initialScale = 0.5f
            ),
            exit = fadeOut() + scaleOut(targetScale = 0f)
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            val baseElevation = 4.adp
            val pressedElevation = 1.adp
            val elevation = if (isPressed) pressedElevation else baseElevation

            val targetOffset = if (isPressed) 4.adp else 0.adp
            val offsetY by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = spring(stiffness = Spring.StiffnessHigh),
                label = "letterOffsetY"
            )

            Box(
                modifier = Modifier
                    .offset(y = offsetY)
                    .shadow(
                        elevation = elevation,
                        shape = RoundedCornerShape(16.adp),
                        ambientColor = Color.Black.copy(alpha = 0.2f),
                        spotColor = Color.Black.copy(alpha = 0.3f)
                    )
                    .clip(RoundedCornerShape(16.adp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        width = 1.adp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(16.adp)
                    )
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClick()
                        }
                    )
                    .aspectRatio(1f / 1.1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter.char.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 22.asp,
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Empty footprint when letter is used
        AnimatedVisibility(
            visible = letter.status == LetterStatus.USED,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(16.adp)
                    .background(
                        MaterialTheme.colorScheme.outlineVariant.copy(0.5f),
                        CircleShape
                    )
            )
        }
    }
}

@Preview(name = "Light - Available")
@Composable
private fun LetterBankItemLightPreview() {
    WordVibeTheme(darkTheme = false) {
        LetterBankItem(
            letter = Letter(id = "1", char = 'A', status = LetterStatus.AVAILABLE),
            onClick = {},
            modifier = Modifier.size(60.adp, 66.adp)
        )
    }
}

@Preview(name = "Dark - Used")
@Composable
private fun LetterBankItemDarkPreview() {
    WordVibeTheme(darkTheme = true) {
        LetterBankItem(
            letter = Letter(id = "1", char = 'B', status = LetterStatus.USED),
            onClick = {},
            modifier = Modifier.size(60.adp, 66.adp)
        )
    }
}


