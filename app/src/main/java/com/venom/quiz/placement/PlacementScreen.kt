package com.venom.quiz.placement

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venom.wordvibe.R
import com.venom.domain.model.PlacementResult
import com.venom.quiz.ui.components.AdaptiveQuizCard
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp

@Composable
fun PlacementScreen(
    onComplete: (PlacementResult) -> Unit,
    onBack: () -> Unit,
    viewModel: PlacementViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.start() }

    // ADR-0010: abandoning the test closes it with no result and no persistence.
    LaunchedEffect(state.isAbandoned) {
        if (state.isAbandoned) onBack()
    }

    AnimatedContent(
        targetState = result != null,
        transitionSpec = {
            fadeIn() + slideInVertically { it / 3 } togetherWith
                    fadeOut() + slideOutVertically { -it / 3 }
        },
        label = "placement_content"
    ) { showResult ->
        if (showResult && result != null) {
            PlacementResultScreen(
                result = result!!,
                onStart = { onComplete(result!!) }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    AdaptiveQuizCard(
                        state = state,
                        onSelect = viewModel::selectOption,
                        onSkip = viewModel::skip,
                        onBack = onBack
                    )
                }
            }
        }
    }
}

// Result Screen 

@Composable
private fun PlacementResultScreen(
    result: PlacementResult,
    onStart: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colorScheme.surface, colorScheme.surfaceContainerLow)
                )
            )
            .padding(24.adp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.School,
            contentDescription = null,
            modifier = Modifier.size(64.adp),
            tint = colorScheme.primary
        )

        Spacer(Modifier.height(20.adp))

        Text(
            text = stringResource(R.string.placement_result_title),
            fontSize = 26.asp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface
        )

        Spacer(Modifier.height(8.adp))

        Text(
            text = stringResource(R.string.placement_result_subtitle),
            fontSize = 14.asp,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.adp))

        // Assigned Level Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.adp),
            color = colorScheme.primaryContainer
        ) {
            Column(
                modifier = Modifier.padding(24.adp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.placement_result_level_label),
                    fontSize = 14.asp,
                    color = colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.adp))
                Text(
                    text = result.estimatedCefr,
                    fontSize = 40.asp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(4.adp))
                Text(
                    text = result.assignedLevel.id.replaceFirstChar { it.uppercase() },
                    fontSize = 16.asp,
                    color = colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(16.adp))

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.adp)
        ) {
            StatCard(
                icon = Icons.Rounded.CheckCircle,
                label = stringResource(R.string.placement_result_correct_label),
                value = stringResource(R.string.score_correct_of_total, result.totalCorrect, result.totalQuestions),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Rounded.TrendingUp,
                label = stringResource(R.string.placement_result_duration_label),
                value = stringResource(R.string.duration_seconds, result.durationMs / 1000),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(32.adp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.adp),
            shape = RoundedCornerShape(14.adp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary
            )
        ) {
            Text(
                text = stringResource(R.string.placement_result_cta),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.asp
            )
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.adp),
        color = colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(16.adp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.adp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(24.adp)
            )
            Text(
                text = value,
                fontSize = 18.asp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            Text(
                text = label,
                fontSize = 12.asp,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}
