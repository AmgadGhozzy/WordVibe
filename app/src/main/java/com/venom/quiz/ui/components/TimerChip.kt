package com.venom.quiz.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp
import com.venom.wordvibe.R

@Composable
fun TimerChip(timeRemaining: Int) {
    val colorScheme = MaterialTheme.colorScheme
    val isWarning = timeRemaining <= 10

    val backgroundColor = if (isWarning) {
        colorScheme.errorContainer
    } else {
        colorScheme.surfaceContainerHighest
    }

    val contentColor = if (isWarning) {
        colorScheme.error
    } else {
        colorScheme.onSurfaceVariant
    }

    // Gentle pulse for warning
    val scale by rememberInfiniteTransition().animateFloat(
        initialValue = 1f,
        targetValue = if (isWarning) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        modifier = Modifier.scale(scale),
        shape = RoundedCornerShape(20.adp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.adp, vertical = 6.adp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.adp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Timer,
                tint = contentColor,
                contentDescription = stringResource(R.string.timer_description),
                modifier = Modifier.size(14.adp)
            )
            Text(
                text = "${timeRemaining}s",
                color = contentColor,
                fontSize = 13.asp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
