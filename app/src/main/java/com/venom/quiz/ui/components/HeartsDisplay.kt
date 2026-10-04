package com.venom.quiz.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp
import com.venom.wordvibe.R

@Composable
fun HeartsDisplay(hearts: Int) {
    val colorScheme = MaterialTheme.colorScheme
    val scale = remember { Animatable(1f) }

    LaunchedEffect(hearts) {
        scale.animateTo(1.3f, animationSpec = tween(100))
        scale.animateTo(1f, animationSpec = tween(100))
    }

    Surface(
        shape = RoundedCornerShape(20.adp),
        color = colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.adp, vertical = 6.adp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.adp)
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = stringResource(R.string.hearts_description),
                tint = colorScheme.error,
                modifier = Modifier
                    .size(14.adp)
                    .scale(scale.value)
            )
            Text(
                text = hearts.toString(),
                color = colorScheme.error,
                fontSize = 13.asp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
