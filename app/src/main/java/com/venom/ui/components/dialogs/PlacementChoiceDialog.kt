package com.venom.ui.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.venom.wordvibe.R
import com.venom.ui.components.buttons.GlassOutlinedButton
import com.venom.ui.components.buttons.GradientActionButton
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp
import com.venom.wordvibe.ui.designsystem.WordVibeTheme

@Composable
fun PlacementChoiceDialog(
    onStartFromBeginning: () -> Unit,
    onTakePlacementTest: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.adp),
            color = colorScheme.surface,
            tonalElevation = 6.adp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.adp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.placement_welcome),
                    fontSize = 22.asp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )

                Spacer(Modifier.height(8.adp))

                Text(
                    text = stringResource(R.string.placement_intro),
                    fontSize = 14.asp,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.adp))

                GradientActionButton(
                    text = stringResource(R.string.placement_cta_test),
                    onClick = onTakePlacementTest,
                    leadingIcon = R.drawable.ic_question_bold
                )

                Spacer(Modifier.height(8.adp))

                Text(
                    text = stringResource(R.string.placement_cta_test_hint),
                    fontSize = 12.asp,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.adp))

                GlassOutlinedButton(
                    text = stringResource(R.string.placement_cta_beginner),
                    onClick = onStartFromBeginning,
                    leadingIcon = R.drawable.icon_play
                )

                Spacer(Modifier.height(4.adp))

                Text(
                    text = stringResource(R.string.placement_cta_beginner_hint),
                    fontSize = 12.asp,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlacementChoiceDialogPreview() {
    WordVibeTheme(darkTheme = true) {
        PlacementChoiceDialog(
            onStartFromBeginning = {},
            onTakePlacementTest = {},
            onDismiss = {}
        )
    }
}
