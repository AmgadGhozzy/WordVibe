package com.venom.stackcard.ui.components.insights

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp

/**
 * Tab navigation bar for the Insights bottom sheet.
 *
 * @param activeTab Currently selected tab
 * @param onTabChange Callback when a tab is selected
 * @param modifier Modifier for styling
 */
@Composable
fun InsightsTabs(
    activeTab: InsightsTab,
    onTabChange: (InsightsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.adp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(6.adp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        InsightsTab.entries.forEach { tab ->
            key(tab) {
                TabItem(
                    tab = tab,
                    isActive = activeTab == tab,
                    onClick = { onTabChange(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: InsightsTab,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(
        targetState = isActive
    )

    val backgroundColor by transition.animateColor(
        transitionSpec = { tween(durationMillis = 150) }
    ) { active ->
        if (active) MaterialTheme.colorScheme.surfaceContainerHighest
        else MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val textColor by transition.animateColor(
        transitionSpec = { tween(durationMillis = 150) }
    ) { active ->
        if (active) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.adp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.adp, horizontal = 10.adp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tab.title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.asp
            ),
            color = textColor,
            maxLines = 1
        )
    }
}

enum class InsightsTab(val title: String) {
    OVERVIEW("Overview"),
    RELATIONS("Relations"),
    USAGE("Usage"),
    LANGUAGES("Languages")
}