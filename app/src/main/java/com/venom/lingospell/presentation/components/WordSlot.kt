package com.venom.lingospell.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.venom.domain.model.AppTheme
import com.venom.lingospell.domain.Letter
import com.venom.lingospell.domain.LetterStatus
import com.venom.lingospell.domain.Slot
import com.venom.lingospell.domain.SlotStatus
import com.venom.ui.components.common.adp
import com.venom.wordvibe.ui.designsystem.WordVibeTheme
import com.venom.designsystem.theme.synapse
import com.venom.designsystem.theme.SemanticColors

enum class SlotVisualState {
    EMPTY,
    ACTIVE,
    FILLED,
    CORRECT,
    ERROR,
    MASTERED
}

@Composable
fun WordSlot(
    slot: Slot,
    isActive: Boolean,
    isMastered: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val semantic = MaterialTheme.synapse.semantic

    val visualState = when {
        isMastered -> SlotVisualState.MASTERED
        slot.status == SlotStatus.CORRECT -> SlotVisualState.CORRECT
        slot.status == SlotStatus.ERROR -> SlotVisualState.ERROR
        isActive && slot.letter == null && !isMastered -> SlotVisualState.ACTIVE
        slot.status == SlotStatus.FILLED -> SlotVisualState.FILLED
        else -> SlotVisualState.EMPTY
    }

    val style = getSlotStyle(visualState, MaterialTheme.colorScheme, semantic)
    val isKeyboardStyle = when (visualState) {
        SlotVisualState.CORRECT,
        SlotVisualState.ERROR,
        SlotVisualState.MASTERED -> true

        else -> false
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetOffsetY = if (isPressed) style.offsetY + 2.adp else style.offsetY
    val offsetY by animateDpAsState(
        targetValue = targetOffsetY,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "offsetY"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) style.scale * 0.95f else style.scale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Box(
        modifier = modifier
            .size(64.adp, 80.adp)
            .offset(y = offsetY)
            .scale(scale)
            .then(
                if (isKeyboardStyle) {
                    val elev = if (isPressed) 2.adp else 4.adp
                    Modifier.shadow(
                        elevation = elev,
                        shape = RoundedCornerShape(12.adp),
                        ambientColor = style.shadowColor,
                        spotColor = style.shadowColor
                    )
                } else {
                    Modifier.shadow(
                        elevation = style.elevation,
                        shape = RoundedCornerShape(12.adp),
                        ambientColor = style.shadowColor,
                        spotColor = style.shadowColor
                    )
                }
            )
            .clip(RoundedCornerShape(12.adp))
            .background(style.backgroundColor)
            .border(
                width = if (isKeyboardStyle) 0.adp else 1.adp,
                color = style.borderColor,
                shape = RoundedCornerShape(12.adp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = slot.letter,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.5f)) togetherWith
                        (fadeOut() + scaleOut(targetScale = 0.5f))
            },
            label = "letterContent"
        ) { letter ->
            if (letter != null) {
                Text(
                    text = letter.char.toString(),
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = style.textColor
                )
            } else if (visualState == SlotVisualState.EMPTY) {
                Box(
                    modifier = Modifier
                        .size(6.adp)
                        .background(colorScheme.outline.copy(0.6f), CircleShape)
                )
            }
        }
    }
}


private data class SlotStyle(
    val backgroundColor: Color,
    val borderColor: Color,
    val textColor: Color,
    val shadowColor: Color,
    val elevation: Dp,
    val offsetY: Dp,
    val scale: Float
)

@Composable
private fun getSlotStyle(
    state: SlotVisualState,
    colorScheme: androidx.compose.material3.ColorScheme,
    semantic: SemanticColors
): SlotStyle {
    return when (state) {
        SlotVisualState.ACTIVE -> SlotStyle(
            backgroundColor = semantic.primaryContainer,
            borderColor = colorScheme.primary,
            textColor = colorScheme.primary,
            shadowColor = colorScheme.primary.copy(0.3f),
            elevation = 12.adp,
            offsetY = (-5).adp,
            scale = 1.05f
        )

        SlotVisualState.MASTERED -> SlotStyle(
            backgroundColor = semantic.gold,
            borderColor = Color.Transparent,
            textColor = colorScheme.onPrimary,
            shadowColor = semantic.goldContainer,
            elevation = 4.adp,
            offsetY = 0.adp,
            scale = 1f
        )

        SlotVisualState.CORRECT -> SlotStyle(
            backgroundColor = semantic.success,
            borderColor = Color.Transparent,
            textColor = semantic.onSuccess,
            shadowColor = semantic.successContainer,
            elevation = 4.adp,
            offsetY = (-2).adp,
            scale = 1f
        )

        SlotVisualState.ERROR -> SlotStyle(
            backgroundColor = colorScheme.error,
            borderColor = Color.Transparent,
            textColor = colorScheme.onError,
            shadowColor = colorScheme.errorContainer,
            elevation = 2.adp,
            offsetY = (-2).adp,
            scale = 1f
        )

        SlotVisualState.FILLED -> SlotStyle(
            backgroundColor = colorScheme.surfaceContainerHigh,
            borderColor = colorScheme.outlineVariant,
            textColor = colorScheme.onSurfaceVariant,
            shadowColor = colorScheme.primary.copy(0.35f),
            elevation = 2.adp,
            offsetY = (-1).adp,
            scale = 1f
        )

        SlotVisualState.EMPTY -> SlotStyle(
            backgroundColor = colorScheme.surfaceContainerLow,
            borderColor = colorScheme.outlineVariant,
            textColor = Color.Transparent,
            shadowColor = Color.Transparent,
            elevation = 1.adp,
            offsetY = 0.adp,
            scale = 1f
        )
    }
}

@Preview(name = "Light - Row", showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
private fun WordSlotRowLightPreview() {
    WordVibeTheme(darkTheme = false) {
        Row(modifier = Modifier.padding(20.adp)) {
            // Empty
            WordSlot(
                slot = Slot("1", null, false, SlotStatus.EMPTY),
                isActive = false, isMastered = false, onClick = {}
            )
            // Active
            WordSlot(
                slot = Slot("2", null, false, SlotStatus.EMPTY),
                isActive = true, isMastered = false, onClick = {},
                modifier = Modifier.padding(horizontal = 8.adp)
            )
            // Filled
            WordSlot(
                slot = Slot("3", Letter("L1", 'A', LetterStatus.USED), false, SlotStatus.FILLED),
                isActive = false, isMastered = false, onClick = {},
                modifier = Modifier.padding(horizontal = 8.adp)
            )
            // Correct
            WordSlot(
                slot = Slot("4", Letter("L2", 'B', LetterStatus.USED), true, SlotStatus.CORRECT),
                isActive = false, isMastered = false, onClick = {},
                modifier = Modifier.padding(horizontal = 8.adp)
            )
            // Error
            WordSlot(
                slot = Slot("5", Letter("L3", 'E', LetterStatus.USED), false, SlotStatus.ERROR),
                isActive = false, isMastered = false, onClick = {},
                modifier = Modifier.padding(horizontal = 8.adp)
            )
        }
    }
}



