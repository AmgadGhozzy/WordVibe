package com.venom.stackcard.ui.components.flashcard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import com.venom.domain.model.LanguageOption
import com.venom.domain.model.WordMaster
import com.venom.stackcard.ui.components.mastery.CardBack
import com.venom.stackcard.ui.components.mastery.CardFront
import com.venom.ui.components.common.adp

/**
 * 3D flip card using alpha + zIndex for smooth visibility transitions.
 *
 * @param word WordMaster instance with all card data
 * @param showFront Whether front face is visible (derived from rotation thresholds)
 * @param isBookmarked Bookmark state
 * @param isHintRevealed Whether blur hint on front is revealed
 * @param pinnedLanguage Optional language to show on back
 * @param onSpeak TTS callback
 * @param onBookmarkToggle Bookmark toggle callback
 * @param onRevealHint Hint reveal callback
 */
@Composable
fun WordCard(
    word: WordMaster,
    showFront: Boolean,
    isBookmarked: Boolean,
    isHintRevealed: Boolean,
    pinnedLanguage: LanguageOption?,
    onSpeak: (String) -> Unit,
    onBookmarkToggle: () -> Unit,
    onRevealHint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cornerRadiusDp = 32.adp
    val cardShape = remember(cornerRadiusDp) { RoundedCornerShape(cornerRadiusDp) }

    Box(modifier = modifier.clip(cardShape)) {

                                Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (showFront) 1f else 0f)
                .graphicsLayer { alpha = if (showFront) 1f else 0f }
                .pointerInput(showFront) {
                                        if (!showFront) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                    .changes.forEach { it.consume() }
                            }
                        }
                    }
                }
        ) {
            CardFront(
                word = word,
                isBookmarked = isBookmarked,
                isHintRevealed = isHintRevealed,
                onSpeak = onSpeak,
                onBookmarkToggle = onBookmarkToggle,
                onRevealHint = onRevealHint
            )
        }

                                Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (showFront) 0f else 1f)
                .graphicsLayer {
                    alpha = if (!showFront) 1f else 0f
                    rotationY = 180f
                }
                .pointerInput(showFront) {
                                        if (showFront) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                    .changes.forEach { it.consume() }
                            }
                        }
                    }
                }
        ) {
            CardBack(
                word = word,
                isBookmarked = isBookmarked,
                pinnedLanguage = pinnedLanguage,
                onSpeak = onSpeak,
                onBookmarkToggle = onBookmarkToggle
            )
        }
    }
}