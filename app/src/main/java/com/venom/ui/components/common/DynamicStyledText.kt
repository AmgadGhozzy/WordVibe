package com.venom.ui.components.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.venom.utils.getTextDirection

@Composable
fun DynamicStyledText(
    text: Any,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Unspecified,
    minFontSize: Int = 22,
    maxFontSize: Int = 32,
    lineHeight: Int = 30,
    letterSpacing: Int = 0,
    maxLines: Int = Int.MAX_VALUE,
    fontWeight: FontWeight = FontWeight.Black,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    val textString = text.toString()

    val dynamicFontSize = remember(textString.length, minFontSize, maxFontSize) {
        (maxFontSize - (textString.length / 6)).coerceAtLeast(minFontSize)
    }.asp

    val textDirection = remember(textString) { getTextDirection(textString) }

    val adaptiveLineHeight = lineHeight.asp
    val adaptiveLetterSpacing = letterSpacing.asp

    val customStyle = remember(
        dynamicFontSize,
        color,
        adaptiveLineHeight,
        adaptiveLetterSpacing,
        fontWeight,
        textDirection
    ) {
        TextStyle(
            fontSize = dynamicFontSize,
            color = color,
            lineHeight = adaptiveLineHeight,
            letterSpacing = adaptiveLetterSpacing,
            fontWeight = fontWeight,
            textDirection = textDirection,
            shadow = Shadow(
                color = color.copy(0.2f),
                offset = Offset(2f, 2f),
                blurRadius = 4f
            )
        )
    }

    SelectionContainer {
        when (text) {
            is AnnotatedString -> {
                Text(
                    text = text,
                    textAlign = textAlign,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = maxLines,
                    style = customStyle,
                    modifier = modifier.padding(4.adp)
                )
            }

            is String -> {
                Text(
                    text = text,
                    textAlign = textAlign,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = maxLines,
                    style = customStyle,
                    modifier = modifier.padding(4.adp)
                )
            }
        }
    }
}