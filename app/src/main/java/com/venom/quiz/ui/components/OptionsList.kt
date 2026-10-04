package com.venom.quiz.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.venom.ui.components.common.adp

@Composable
fun OptionsList(
    options: List<String>,
    selectedOption: String?,
    isAnswered: Boolean,
    correctAnswer: String?,
    onOptionSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.adp)) {
        val normalizedOptions = options
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        normalizedOptions.forEachIndexed { index, option ->
            OptionItem(
                option = option,
                label = ((('A'.code + index).coerceAtMost('E'.code)).toChar()).toString(),
                isSelected = option == selectedOption,
                isCorrect = option == correctAnswer,
                isAnswered = isAnswered,
                onClick = { onOptionSelected(option) }
            )
        }
    }
}