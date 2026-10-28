package com.venom.ui.components.buttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.venom.wordvibe.R
import com.venom.ui.components.common.adp

/**
 * Standard close button with consistent styling across the app
 *
 * @param onClick Callback invoked when button is clicked
 * @param modifier Modifier for the button
 * @param contentDescription Accessibility description (defaults to "Close")
 * @param size Size of the button (defaults to 48.adp)
 */
@Composable
fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(R.string.action_close),
    size: Dp = 48.adp,
    iconSize: Dp = 20.adp,
) {
    CustomFilledIconButton(
        icon = Icons.AutoMirrored.Rounded.ArrowForwardIos,
        modifier = modifier,
        onClick = onClick,
        contentDescription = contentDescription,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface.copy(0.7f)
        ),
        size = size,
        iconSize = iconSize
    )
}
