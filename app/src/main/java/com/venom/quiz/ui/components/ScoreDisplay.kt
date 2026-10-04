package com.venom.quiz.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp
import com.venom.wordvibe.R

@Composable
fun ScoreDisplay(score: Int) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(20.adp),
        color = colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.adp, vertical = 6.adp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.adp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = stringResource(R.string.score),
                tint = colorScheme.onPrimaryContainer,
                modifier = Modifier.size(14.adp)
            )
            Text(
                text = score.toString(),
                color = colorScheme.onPrimaryContainer,
                fontSize = 13.asp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
