package com.venom.stackcard.ui.components.mastery

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import com.venom.domain.model.AppTheme
import com.venom.wordvibe.R
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp
import com.venom.wordvibe.ui.designsystem.WordVibeTheme
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom action bar for word card interactions.
 */
@Composable
fun ActionBar(
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onPractice: () -> Unit,
    onInfoClick: () -> Unit,
    onKnown: () -> Unit,
    onAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.adp),
        horizontalArrangement = Arrangement.spacedBy(16.adp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isFlipped) {
            // Placeholder to keep spacing or just centered buttons
            Box(modifier = Modifier.size(56.adp)) // Spacer for symmetry if needed, or just let them center
            PracticeButton(
                onClick = onPractice,
                modifier = Modifier.weight(1f, fill = false)
            )
            CircularButton(
                icon = R.drawable.ic_refresh_cw,
                contentDescription = stringResource(R.string.mastery_flip),
                onClick = onFlip
            )
        } else {
            CircularButton(
                icon = Icons.Default.Close,
                contentDescription = "Again",
                onClick = onAgain,
                tint = Color(0xFFEF4444) // Red-500
            )
            PracticeButton(
                onClick = onPractice,
                modifier = Modifier.weight(1f, fill = false)
            )
            CircularButton(
                icon = Icons.Default.Check,
                contentDescription = "Good",
                onClick = onKnown,
                tint = Color(0xFF22C55E) // Green-500
            )
            CircularButton(
                icon = R.drawable.ic_list_dashes,
                contentDescription = stringResource(R.string.mastery_info),
                onClick = onInfoClick
            )
        }
    }
}

@Composable
private fun CircularButton(
    icon: Any, // Int for resId or ImageVector
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "circular_button_scale"
    )

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(56.adp)
            .scale(scale)
            .shadow(
                elevation = 10.adp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.20f),
                spotColor = Color.Black.copy(alpha = 0.30f)
            )
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.80f))
            .border(1.adp, Color.White.copy(0.12f), CircleShape),
        interactionSource = interactionSource
    ) {
        val finalTint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant
        when (icon) {
            is Int -> {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = contentDescription,
                    modifier = Modifier.size(24.adp),
                    tint = finalTint
                )
            }
            is ImageVector -> {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(24.adp),
                    tint = finalTint
                )
            }
        }
    }
}

@Composable
private fun PracticeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f)

    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.adp)
            .scale(scale)
            .shadow(
                elevation = 10.adp,
                shape = RoundedCornerShape(28.adp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(0.25f),
                spotColor = MaterialTheme.colorScheme.primary.copy(0.35f)
            ),
        shape = RoundedCornerShape(28.adp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.adp),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
                    ),
                    shape = RoundedCornerShape(28.adp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.adp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_feather),
                    contentDescription = null,
                    modifier = Modifier.size(20.adp),
                    tint = Color.White
                )
                Text(
                    text = stringResource(R.string.mastery_practice),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.asp,
                    color = Color.White
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun ActionBarPreview() {
    WordVibeTheme(darkTheme = true) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(16.adp)
        ) {
            ActionBar(
                isFlipped = false,
                onFlip = {},
                onPractice = {},
                onInfoClick = {},
                onKnown = {},
                onAgain = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF1F5F9)
@Composable
private fun ActionBarPreviewLight() {
    WordVibeTheme(darkTheme = false) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(16.adp)
        ) {
            ActionBar(
                isFlipped = true,
                onFlip = {},
                onPractice = {},
                onInfoClick = {},
                onKnown = {},
                onAgain = {}
            )
        }
    }
}


