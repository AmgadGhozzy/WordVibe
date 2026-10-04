package com.venom.quiz.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp
import com.venom.wordvibe.R

@Composable
fun OptionItem(
    option: String,
    label: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    isAnswered: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val scale by animateFloatAsState(
        targetValue = if (isSelected && !isAnswered) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f)
    )

    // Determine colors based on state
    val isCorrectOption = isAnswered && isCorrect
    val isIncorrectSelected = isAnswered && isSelected && !isCorrect

    val backgroundColor = when {
        isCorrectOption -> colorScheme.tertiaryContainer.copy(0.4f)
        isIncorrectSelected -> colorScheme.errorContainer.copy(0.4f)
        isSelected -> colorScheme.primaryContainer.copy(0.4f)
        else -> colorScheme.surface
    }

    val borderColor = when {
        isCorrectOption -> colorScheme.tertiary
        isIncorrectSelected -> colorScheme.error
        isSelected -> colorScheme.primary
        else -> colorScheme.outlineVariant
    }

    val labelBackgroundColor = when {
        isCorrectOption -> colorScheme.tertiary
        isIncorrectSelected -> colorScheme.error
        isSelected -> colorScheme.primary
        else -> colorScheme.surfaceContainerHighest
    }

    val labelTextColor = when {
        isCorrectOption -> colorScheme.onTertiary
        isIncorrectSelected -> colorScheme.onError
        isSelected -> colorScheme.onPrimary
        else -> colorScheme.onSurfaceVariant
    }

    val labelContent = when {
        isCorrectOption -> stringResource(R.string.correct_symbol)
        isIncorrectSelected -> stringResource(R.string.incorrect_symbol)
        else -> label
    }
    val stateDescriptionText = when {
        isCorrectOption -> "Correct answer"
        isIncorrectSelected -> "Incorrect answer"
        isSelected -> "Selected"
        else -> "Not selected"
    }

    Surface(
        onClick = { if (!isAnswered) onClick() },
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .semantics {
                role = Role.RadioButton
                selected = isSelected
                contentDescription = "Option $label: $option"
                stateDescription = stateDescriptionText
            },
        shape = RoundedCornerShape(16.adp),
        color = backgroundColor,
        border = BorderStroke(
            width = if (isAnswered && (isSelected || isCorrectOption)) 2.adp else 1.adp,
            color = borderColor
        ),
        tonalElevation = if (isSelected) 1.adp else 0.adp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.adp, vertical = 14.adp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.adp)
        ) {
            // Label Circle
            Box(
                modifier = Modifier
                    .size(36.adp)
                    .clip(CircleShape)
                    .background(labelBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labelContent,
                    color = labelTextColor,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Option Text
            Text(
                text = option,
                color = colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun OptionsList(
    options: List<String>,
    selectedOption: String?,
    isAnswered: Boolean,
    correctAnswer: String,
    onOptionSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.adp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        options.forEachIndexed { index, option ->
            OptionItem(
                option = option,
                label = ('A' + index).toString(),
                isSelected = option == selectedOption,
                isCorrect = option == correctAnswer,
                isAnswered = isAnswered,
                onClick = { onOptionSelected(option) }
            )
        }
    }
}

