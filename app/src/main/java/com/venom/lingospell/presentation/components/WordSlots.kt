package com.venom.lingospell.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import com.venom.domain.model.AppTheme
import com.venom.lingospell.domain.FeedbackState
import com.venom.lingospell.domain.Letter
import com.venom.lingospell.domain.LetterStatus
import com.venom.lingospell.domain.Slot
import com.venom.lingospell.domain.SlotStatus
import com.venom.ui.components.common.adp
import com.venom.designsystem.theme.SynapseTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordSlots(
    slots: List<Slot>,
    onSlotClick: (Int) -> Unit,
    shakeTrigger: Int,
    feedback: FeedbackState,
    modifier: Modifier = Modifier
) {
    val firstEmptyIndex = slots.indexOfFirst { it.status == SlotStatus.EMPTY }
    val isMastered = feedback == FeedbackState.MASTERED

    // Shake animation state
    var shakeState by remember { mutableIntStateOf(0) }
    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger > 0) {
            shakeState++
        }
    }

    val shake8 = 8.adp.value
    val shake4 = 4.adp.value

    val shakeOffset by animateFloatAsState(
        targetValue = 0f,
        animationSpec = keyframes {
            durationMillis = 400
            0f at 0
            (-shake8) at 50
            shake8 at 100
            (-shake8) at 150
            shake8 at 200
            (-shake4) at 250
            shake4 at 300
            0f at 400
        },
        label = "shakeOffset"
    )

    val currentShakeOffset = if (shakeState > 0 && shakeTrigger > 0) shakeOffset.toInt() else 0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.adp)
            .offset { IntOffset(x = currentShakeOffset, y = 0) },
        contentAlignment = Alignment.Center
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.adp),
            verticalArrangement = Arrangement.spacedBy(12.adp),
            modifier = Modifier.padding(horizontal = 8.adp)
        ) {
            slots.forEachIndexed { index, slot ->
                val isActive = index == firstEmptyIndex && feedback == FeedbackState.IDLE

                WordSlot(
                    slot = slot,
                    isActive = isActive,
                    isMastered = isMastered,
                    onClick = { onSlotClick(index) }
                )
            }
        }
    }
}

// Previews
@Preview(showBackground = true, backgroundColor = 0xFFF8FAFC, name = "Light Mode")
@Composable
private fun WordSlotsLightPreview() {
    SynapseTheme(darkTheme = false) {
        val slots = listOf(
            Slot("0", Letter("l1", 'C', LetterStatus.USED), false, SlotStatus.FILLED),
            Slot("1", null, false, SlotStatus.EMPTY),
            Slot("2", Letter("l2", 'R', LetterStatus.USED), false, SlotStatus.FILLED)
        )
        WordSlots(
            slots = slots,
            onSlotClick = {},
            shakeTrigger = 0,
            feedback = FeedbackState.IDLE
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, name = "Dark Mode")
@Composable
private fun WordSlotsDarkPreview() {
    SynapseTheme(darkTheme = true) {
        val slots = listOf(
            Slot("0", Letter("l1", 'C', LetterStatus.USED), false, SlotStatus.FILLED),
            Slot("1", null, false, SlotStatus.EMPTY),
            Slot("2", Letter("l2", 'R', LetterStatus.USED), false, SlotStatus.FILLED)
        )
        WordSlots(
            slots = slots,
            onSlotClick = {},
            shakeTrigger = 0,
            feedback = FeedbackState.IDLE
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, name = "Dark - Correct State")
@Composable
private fun WordSlotsCorrectDarkPreview() {
    SynapseTheme(darkTheme = true) {
        val slots = listOf(
            Slot("0", Letter("l1", 'C', LetterStatus.USED), true, SlotStatus.CORRECT),
            Slot("1", Letter("l2", 'A', LetterStatus.USED), true, SlotStatus.CORRECT),
            Slot("2", Letter("l3", 'R', LetterStatus.USED), true, SlotStatus.CORRECT)
        )
        WordSlots(
            slots = slots,
            onSlotClick = {},
            shakeTrigger = 0,
            feedback = FeedbackState.SUCCESS
        )
    }
}

