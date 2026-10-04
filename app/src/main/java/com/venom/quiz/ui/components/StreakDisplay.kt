package com.venom.quiz.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp

@Composable
fun StreakDisplay(streak: Int) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(20.adp),
        color = colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.adp, vertical = 6.adp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.adp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Whatshot,
                contentDescription = "Streak",
                tint = colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.adp)
            )
            Text(
                text = streak.toString(),
                color = colorScheme.onSecondaryContainer,
                fontSize = 13.asp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}