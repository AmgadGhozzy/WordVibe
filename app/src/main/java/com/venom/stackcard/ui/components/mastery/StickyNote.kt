package com.venom.stackcard.ui.components.mastery

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp
import com.venom.ui.components.other.BiDiFormatter
import com.venom.domain.model.AppTheme
import com.venom.wordvibe.core.theme.PlaypenSans
import com.venom.wordvibe.ui.designsystem.WordVibeTheme
import kotlin.random.Random

enum class StickyVariant { YELLOW, AMBER, ROSE, BLUE }

private data class StickyColors(
    val bg: Color,
    val border: Color,
    val text: Color,
    val label: Color,
    val tape: Color,
)

private data class StickyConfig(
    val rotation: Float,
    val tapeOffset: Float,
    val variant: StickyVariant
)

private fun getStickyColors(variant: StickyVariant): StickyColors = when (variant) {
    StickyVariant.YELLOW -> StickyColors(
        bg = Color(0xFFFEF08A),
        border = Color(0xFFFDE047),
        text = Color(0xFF713F12),
        label = Color(0xFFA16207),
        tape = Color(0xFFFEF9C3)
    )
    StickyVariant.AMBER -> StickyColors(
        bg = Color(0xFFFDE68A),
        border = Color(0xFFFCD34D),
        text = Color(0xFF78350F),
        label = Color(0xFFB45309),
        tape = Color(0xFFFEF3C7)
    )
    StickyVariant.ROSE -> StickyColors(
        bg = Color(0xFFFECDD3),
        border = Color(0xFFFDA4AF),
        text = Color(0xFF881337),
        label = Color(0xFFBE123C),
        tape = Color(0xFFFFE4E6)
    )
    StickyVariant.BLUE -> StickyColors(
        bg = Color(0xFFBAE6FD),
        border = Color(0xFF7DD3FC),
        text = Color(0xFF0C4A6E),
        label = Color(0xFF0369A1),
        tape = Color(0xFFE0F2FE)
    )
}

/**
 * Sticky Note Card for Memory Hooks - REALISTIC GRADIENT VERSION
 */
@Composable
fun StickyNoteCard(
    mnemonicText: String,
    wordId: Any,
    modifier: Modifier = Modifier
) {
    var stickyConfig by remember(wordId) {
        mutableStateOf(
            StickyConfig(
                rotation = Random.nextFloat() * 6f - 3f,
                tapeOffset = Random.nextFloat() * 30f - 15f,
                variant = StickyVariant.entries.random()
            )
        )
    }

    val colorScheme = getStickyColors(stickyConfig.variant)

    val animatedRotation by animateFloatAsState(
        targetValue = stickyConfig.rotation,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Box(
        modifier = modifier
            .padding(horizontal = 8.adp)
            .padding(top = 24.adp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .rotate(animatedRotation)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    stickyConfig = stickyConfig.copy(rotation = Random.nextFloat() * 6f - 3f)
                }
                .shadow(
                    elevation = 8.adp,
                    shape = RoundedCornerShape(10.adp),
                    ambientColor = colorScheme.label.copy(0.2f)
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.bg.copy(1f),
                            colorScheme.bg.copy(0.98f),
                            colorScheme.bg.copy(0.96f),
                            colorScheme.bg.copy(0.94f),
                            colorScheme.bg.copy(0.92f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    ),
                    shape = RoundedCornerShape(10.adp)
                )
                .border(
                    width = 0.5.adp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.border.copy(0.6f),
                            colorScheme.border.copy(0.8f),
                            colorScheme.border.copy(1f)
                        )
                    ),
                    shape = RoundedCornerShape(10.adp)
                )
                .padding(20.adp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.adp)
            ) {
                Text(
                    text = "MEMORY HOOK",
                    fontSize = 10.asp,
                    fontWeight = FontWeight.Black,
                    color = colorScheme.label,
                    letterSpacing = 2.asp
                )

                Text(
                    text = remember(mnemonicText) { BiDiFormatter.format(mnemonicText) },
                    fontSize = 20.asp,
                    color = colorScheme.text,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.asp,
                    fontFamily = PlaypenSans,
                    style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content)
                )
            }
        }

        Box(
            modifier = Modifier
                .offset(x = (stickyConfig.tapeOffset).adp, y = (-12).adp)
                .width(80.adp)
                .height(28.adp)
                .rotate(-2f)
                .alpha(0.8f)
                .align(Alignment.TopCenter)
                .shadow(2.adp, RoundedCornerShape(2.adp), ambientColor = Color.Black.copy(0.1f))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.tape.copy(0.9f),
                            colorScheme.tape.copy(0.7f),
                            colorScheme.tape.copy(0.75f),
                            colorScheme.tape.copy(0.85f)
                        )
                    ),
                    shape = RoundedCornerShape(2.adp)
                )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
fun StickyNotePreview() {
    WordVibeTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(24.adp),
            verticalArrangement = Arrangement.spacedBy(16.adp)
        ) {
            StickyNoteCard(
                mnemonicText = "The word (كلمة) means 'word' in Arabic",
                wordId = 101
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF1F5F9, name = "Light Theme - Paper Texture")
@Composable
fun StickyNotePreviewLight() {
    WordVibeTheme(darkTheme = false) {
        Column(
            modifier = Modifier.padding(24.adp),
            verticalArrangement = Arrangement.spacedBy(16.adp)
        ) {
            StickyNoteCard(
                mnemonicText = "Journey (رحلة) - traveling adventure",
                wordId = 202
            )
        }
    }
}

