package com.venom.lingospell.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.venom.domain.model.AppTheme
import com.venom.lingospell.domain.HintLevel
import com.venom.wordvibe.R
import com.venom.ui.components.common.adp
import com.venom.wordvibe.ui.designsystem.WordVibeTheme

@Composable
fun HintButton(
    hintLevel: HintLevel,
    onHintClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (hintLevel.ordinal >= 3 || !enabled) return

    Row(
        modifier = modifier
            .shadow(4.adp, RoundedCornerShape(24.adp))
            .clip(RoundedCornerShape(24.adp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(
                width = 0.5.adp,
                color = MaterialTheme.colorScheme.primary.copy(0.2f),
                shape = RoundedCornerShape(24.adp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onHintClick
            )
            .padding(horizontal = 16.adp, vertical = 8.adp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.icon_lightbulb),
            contentDescription = "Hint",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(16.adp)
        )
        Spacer(modifier = Modifier.width(8.adp))
        Text(
            text = "Get Hint (${3 - hintLevel.ordinal} left)",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Preview(name = "Light - Hint")
@Composable
private fun HintButtonLightPreview() {
    WordVibeTheme(darkTheme = false) {
        HintButton(hintLevel = HintLevel.NONE, onHintClick = {}, enabled = true)
    }
}

@Preview(name = "Dark - Hint")
@Composable
private fun HintButtonDarkPreview() {
    WordVibeTheme(darkTheme = true) {
        HintButton(hintLevel = HintLevel.LEVEL_1, onHintClick = {}, enabled = true)
    }
}



