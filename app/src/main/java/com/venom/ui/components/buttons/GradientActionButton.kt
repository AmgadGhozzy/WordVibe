package com.venom.ui.components.buttons

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp
import com.venom.designsystem.theme.SynapseTheme

@Composable
fun GradientActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: Int? = null,
    maxWidth: Int = 300,
    height: Int = 56,
    gradientColors: List<Color> = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
    shadowColors: List<Color> = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
    ),
    loadingContent: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val offsetY by animateDpAsState(
        targetValue = if (isPressed) 5.adp else 0.adp,
        animationSpec = tween(80)
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(80)
    )

    Box(
        modifier = modifier
            .widthIn(max = maxWidth.adp)
            .fillMaxWidth()
            .scale(buttonScale)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.adp)
                .offset(y = 5.adp)
                .background(
                    brush = Brush.horizontalGradient(shadowColors),
                    shape = RoundedCornerShape(16.adp)
                )
        )

        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(height.adp)
                .offset(y = offsetY),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.adp),
            shape = RoundedCornerShape(16.adp),
            interactionSource = interactionSource,
            elevation = ButtonDefaults.buttonElevation(0.adp, 0.adp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(gradientColors),
                        shape = RoundedCornerShape(16.adp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!enabled && loadingContent != null) {
                    loadingContent()
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.adp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (leadingIcon != null) {
                            Box(
                                modifier = Modifier
                                    .size(24.adp)
                                    .background(Color.White.copy(0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(leadingIcon),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.adp),
                                    tint = Color.White.copy(0.9f)
                                )
                            }
                        }
                        Text(
                            text = text,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.asp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Gradient Action Button – Light")
@Preview(name = "Gradient Action Button – Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GradientActionButtonLightPreview() {
    SynapseTheme {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            GradientActionButton(
                text = "Start",
                onClick = {}
            )
        }
    }
}

