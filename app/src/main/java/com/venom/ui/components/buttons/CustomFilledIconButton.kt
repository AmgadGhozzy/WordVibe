package com.venom.ui.components.buttons

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.venom.ui.components.common.adp

@Composable
fun CustomFilledIconButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    contentDescription: String,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    size: Dp = 44.adp,
    iconSize: Dp = 20.adp,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        colors = colors
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize)
        )
    }
}
