package com.venom.ui.components.buttons

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.venom.ui.components.common.adp
import com.venom.wordvibe.ui.designsystem.WordVibeTheme

@Composable
fun GlassOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: Int? = null,
    maxWidth: Int = 300,
    height: Int = 50,
    borderColors: List<Color> = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
    ),
    iconTint: Color = MaterialTheme.colorScheme.primary
) {


    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(80)
    )

    val bgColor = if (isDark)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    else
        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)

    val textColor = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary

    Button(
        onClick = onClick,
        modifier = modifier
            .widthIn(max = maxWidth.adp)
            .fillMaxWidth()
            .height(height.adp)
            .scale(buttonScale)
            .border(
                width = 1.2.adp,
                brush = Brush.horizontalGradient(borderColors),
                shape = RoundedCornerShape(16.adp)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(16.adp),
        interactionSource = interactionSource,
        elevation = ButtonDefaults.buttonElevation(0.adp, 0.adp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.adp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    painter = painterResource(leadingIcon),
                    contentDescription = null,
                    modifier = Modifier.size(18.adp),
                    tint = iconTint
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = textColor
            )
        }
    }
}

@Preview(name = "Glass Outlined Button – Light")
@Preview(name = "Glass Outlined Button – Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GlassOutlinedButtonLightPreview() {
    WordVibeTheme {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            GlassOutlinedButton(
                text = "Click me",
                onClick = {},
            )
        }
    }
}


