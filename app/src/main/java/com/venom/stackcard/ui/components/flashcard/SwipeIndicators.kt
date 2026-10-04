package com.venom.stackcard.ui.components.flashcard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import com.venom.ui.components.common.adp
import androidx.compose.ui.graphics.Color
import com.venom.designsystem.theme.synapse
import kotlin.math.abs

@Composable
fun SwipeIndicators(
    animState: CardAnimationState,
    swipeThresholdPx: Float,
    modifier: Modifier = Modifier
) {
    val appearThreshold = 0.15f

    Row(
        modifier = modifier.fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    val progress = (animState.offsetX.value / swipeThresholdPx).coerceIn(-1f, 0f)
                    val absProgress = abs(progress)

                    alpha = if (absProgress > appearThreshold) {
                        ((absProgress - appearThreshold) / (1f - appearThreshold)).coerceIn(0f, 1f)
                    } else 0f

                    scaleX = 0.5f + (absProgress * 0.5f)
                    scaleY = 0.5f + (absProgress * 0.5f)
                }
                .size(56.adp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.9f))
                .padding(12.adp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Forgot",
                tint = Color.White,
                modifier = Modifier.size(32.adp)
            )
        }

        Spacer(modifier = Modifier.width(200.adp))

        Box(
            modifier = Modifier
                .graphicsLayer {
                    val progress = (animState.offsetX.value / swipeThresholdPx).coerceIn(0f, 1f)

                    alpha = if (progress > appearThreshold) {
                        ((progress - appearThreshold) / (1f - appearThreshold)).coerceIn(0f, 1f)
                    } else 0f

                    scaleX = 0.5f + (progress * 0.5f)
                    scaleY = 0.5f + (progress * 0.5f)
                }
                .size(56.adp)
                .clip(CircleShape)
                .background(MaterialTheme.synapse.semantic.success.copy(alpha = 0.9f))
                .padding(12.adp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Remember",
                tint = Color.White,
                modifier = Modifier.size(32.adp)
            )
        }
    }
}
