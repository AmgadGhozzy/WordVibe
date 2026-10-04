package com.venom.stackcard.ui.components.flashcard

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.venom.ui.components.common.adp
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun MasterySwipeableCard(
    animState: CardAnimationState?,
    stackPosition: StackPosition,
    isTopCard: Boolean,
    isFlipped: Boolean,
    flipRotation: Float,
    swipeThresholdPx: Float,
    onFlip: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (showFront: Boolean) -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val cornerRadiusDp = 32.adp
    val paddingDp = 24.adp
    val limitDp = 400.adp
    val screenWidthDp = configuration.screenWidthDp.adp
    val elevationDp = 0.adp

    val cardShape = remember(cornerRadiusDp) { RoundedCornerShape(cornerRadiusDp) }

            val flipAnimatable = remember { Animatable(0f) }

        LaunchedEffect(flipRotation) {
        flipAnimatable.animateTo(flipRotation, OptimizedCardAnimations.FlipAnimationSpec)
    }

                val showFront by remember(flipAnimatable) {
        derivedStateOf {
            val raw = flipAnimatable.value % 360f
            val effective = if (raw < 0f) raw + 360f else raw
            effective <= 90f || effective >= 270f
        }
    }

    val (cardWidth, cardHeight) = remember(screenWidthDp, paddingDp, limitDp, density) {
        val maxWidthPx = with(density) { screenWidthDp.toPx() }
        val paddingPx = with(density) { paddingDp.toPx() }
        val limitPx = with(density) { limitDp.toPx() }
        val cardWidthPx = min(maxWidthPx - paddingPx, limitPx)
        val cardWidthDp = with(density) { cardWidthPx.toDp() }
        Pair(cardWidthDp, cardWidthDp * 1.8f)
    }

    val stackOffsetXDp = stackPosition.offsetXDp.adp
    val stackOffsetYDp = stackPosition.offsetYDp.adp

    val stackOffsetXPx = remember(stackOffsetXDp, density) {
        with(density) { stackOffsetXDp.toPx() }
    }
    val stackOffsetYPx = remember(stackOffsetYDp, density) {
        with(density) { stackOffsetYDp.toPx() }
    }

    val cameraDistance = remember(density) { 12f * density.density }

    val borderCornerRadiusPx = remember(cornerRadiusDp, density) {
        with(density) { cornerRadiusDp.toPx() }
    }
    val borderWidthThinPx = remember(density) { with(density) { 1.dp.toPx() } }
    val borderWidthThickPx = remember(density) { with(density) { 2.dp.toPx() } }

    Card(
        modifier = modifier
            .size(cardWidth, cardHeight)
            .graphicsLayer {
                if (isTopCard && animState != null) {
                    val currentOffsetX = animState.offsetX.value
                    val currentOffsetY = animState.offsetY.value
                    val currentRotation = animState.rotation.value

                    translationX = currentOffsetX + stackOffsetXPx
                    translationY = currentOffsetY + stackOffsetYPx
                    rotationZ = if (isFlipped) -currentRotation else currentRotation

                                        rotationY = flipAnimatable.value

                    val dragDistance = sqrt(
                        currentOffsetX.pow(2) + currentOffsetY.pow(2)
                    )
                    val dynamicScale = (1f - dragDistance * 0.0003f).coerceIn(0.8f, 1f)
                    scaleX = dynamicScale
                    scaleY = dynamicScale
                } else {
                    translationX = stackOffsetXPx
                    translationY = stackOffsetYPx
                    rotationZ = stackPosition.rotation
                    scaleX = stackPosition.scale
                    scaleY = stackPosition.scale
                    rotationY = 0f
                }

                this.cameraDistance = cameraDistance
                clip = false
            }
            .clip(cardShape)
            .drawWithContent {
                drawContent()
                val progress = if (animState != null && isTopCard) {
                    (animState.offsetX.value / swipeThresholdPx).coerceIn(-1f, 1f)
                } else 0f
                val borderColor = CardColors.getBorderColor(progress)
                val strokeWidth = if (abs(progress) > CardColors.PROGRESS_THRESHOLD) {
                    borderWidthThickPx
                } else borderWidthThinPx
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = CornerRadius(borderCornerRadiusPx),
                    style = Stroke(width = strokeWidth)
                )
            }
            .then(
                if (isTopCard) {
                    Modifier
                        .pointerInput(isFlipped) {
                            if (isFlipped) {
                                detectDragGestures(
                                    onDragEnd = { onDragEnd() },
                                    onDragCancel = { onDragEnd() }
                                ) { change, dragAmount ->
                                    change.consume()
                                    val correctedX =
                                        if (isFlipped) -dragAmount.x else dragAmount.x
                                    onDrag(correctedX, dragAmount.y)
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onFlip() })
                        }
                } else Modifier
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = elevationDp)
    ) {
        content(if (isTopCard) showFront else true)
    }
}