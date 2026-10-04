package com.venom.lingospell.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.venom.wordvibe.R
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp
import com.venom.ui.components.other.ConfettiView
import com.venom.designsystem.theme.synapse

@Composable
fun MasteryDialog(
    word: String,
    arabicWord: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        alpha.animateTo(1f, tween(500))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .alpha(alpha.value),
        contentAlignment = Alignment.Center,
    ) {
        ConfettiView()

        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .scale(scale.value)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.adp))
                .padding(32.adp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_trophy),
                contentDescription = null,
                modifier = Modifier.size(80.adp),
                tint = MaterialTheme.synapse.semantic.gold
            )

            Spacer(Modifier.height(16.adp))

            Text(
                text = "MASTERED!",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.asp,
                ),
                color = MaterialTheme.synapse.semantic.gold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.adp))

            Text(
                text = "You've spelled this word correctly 3 times!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.adp))

            Text(
                text = arabicWord,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.adp))

            Text(
                text = word.uppercase(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.asp
                ),
                color = MaterialTheme.synapse.semantic.gold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.adp))

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(56.adp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.synapse.semantic.gold,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.adp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.adp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Continue to Next Word",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Icon(
                        imageVector = Icons.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(20.adp)
                    )
                }
            }
        }
    }
}

