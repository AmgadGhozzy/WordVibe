package com.venom.stackcard.ui.components.mastery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.venom.ui.components.common.adp
import com.venom.wordvibe.R
import com.venom.domain.model.AppTheme
import com.venom.designsystem.theme.SynapseTheme

@Composable
fun CardsProgressIndicator(
    currentIndex: Int,
    totalCards: Int,
    difficultyScore: Int = 5,
    modifier: Modifier = Modifier
) {
    val displayIndex = currentIndex
    val progress = remember(displayIndex, totalCards) {
        if (totalCards > 0) displayIndex.toFloat() / totalCards else 0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.adp))
            .background(surfaceColor.copy(alpha = 0.5f))
            .border(1.adp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(12.adp))
            .padding(horizontal = 12.adp, vertical = 8.adp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.adp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.icon_cards2),
            contentDescription = null,
            modifier = Modifier.size(16.adp),
            tint = primaryColor
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.adp)
        ) {
            AnimatedContent(
                targetState = displayIndex,
                transitionSpec = {
                    (fadeIn() + scaleIn(initialScale = 0.8f))
                        .togetherWith(fadeOut() + scaleOut(targetScale = 1.2f))
                }
            ) { count ->
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = primaryColor
                )
            }

            Text(
                text = " of $totalCards",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.adp)
                .clip(RoundedCornerShape(2.adp))
                .background(primaryColor.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.adp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(primaryColor, primaryColor.copy(alpha = 0.6f))
                        )
                    )
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun CardsProgressIndicatorPreview() {
    SynapseTheme(darkTheme = true) {
        CardsProgressIndicator(currentIndex = 3, totalCards = 10, difficultyScore = 2, modifier = Modifier.padding(16.adp))
    }
}


