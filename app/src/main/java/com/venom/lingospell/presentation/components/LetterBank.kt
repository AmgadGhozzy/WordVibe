package com.venom.lingospell.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.venom.domain.model.AppTheme
import com.venom.lingospell.domain.Letter
import com.venom.lingospell.domain.LetterStatus
import com.venom.wordvibe.R
import com.venom.ui.components.common.adp
import com.venom.wordvibe.ui.designsystem.WordVibeTheme
import com.venom.designsystem.theme.synapse

@Composable
fun LetterBank(
    letters: List<Letter>,
    onLetterClick: (Letter) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleLetters = letters.filter { it.status != LetterStatus.HIDDEN }

    var rotationTrigger by remember { mutableIntStateOf(0) }
    val rotation by animateFloatAsState(
        targetValue = rotationTrigger * -180f,
        animationSpec = tween(500)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 68.adp,
                shape = RoundedCornerShape(
                    topStart = 40.adp,
                    topEnd = 40.adp
                ),
                ambientColor = MaterialTheme.colorScheme.primary,
                spotColor = MaterialTheme.colorScheme.primary
            )
            .clip(
                RoundedCornerShape(
                    topStart = 40.adp,
                    topEnd = 40.adp
                )
            )
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 24.adp, vertical = 24.adp)
    ) {
        // Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.adp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(com.venom.wordvibe.R.string.spelling_available_letters),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        rotationTrigger++
                        onClear()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.adp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.icon_rotate),
                    contentDescription = "Reset",
                    modifier = Modifier
                        .size(14.adp)
                        .rotate(rotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(com.venom.wordvibe.R.string.action_reset),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.adp),
            horizontalArrangement = Arrangement.spacedBy(12.adp),
            verticalArrangement = Arrangement.spacedBy(12.adp),
            contentPadding = PaddingValues(bottom = 8.adp)
        ) {
            itemsIndexed(
                items = visibleLetters,
                key = { _, letter -> letter.id }
            ) { _, letter ->
                LetterBankItem(
                    letter = letter,
                    onClick = { onLetterClick(letter) }
                )
            }
        }
    }
}

@Preview(name = "Light Theme")
@Composable
private fun LetterBankLightPreview() {
    val sampleLetters = listOf(
        Letter("1", 'C', LetterStatus.AVAILABLE),
        Letter("2", 'A', LetterStatus.AVAILABLE),
        Letter("3", 'R', LetterStatus.AVAILABLE),
        Letter("4", 'X', LetterStatus.USED),
        Letter("5", 'Z', LetterStatus.AVAILABLE)
    )
    WordVibeTheme(darkTheme = false) {
        LetterBank(
            letters = sampleLetters,
            onLetterClick = {},
            onClear = {}
        )
    }
}

@Preview(name = "Dark Theme")
@Composable
private fun LetterBankDarkPreview() {
    val sampleLetters = listOf(
        Letter("1", 'C', LetterStatus.AVAILABLE),
        Letter("2", 'A', LetterStatus.AVAILABLE),
        Letter("3", 'R', LetterStatus.AVAILABLE),
        Letter("4", 'X', LetterStatus.USED),
        Letter("5", 'Z', LetterStatus.AVAILABLE)
    )
    WordVibeTheme(darkTheme = true) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            LetterBank(
                letters = sampleLetters,
                onLetterClick = {},
                onClear = {}
            )
        }
    }
}



