package com.venom.stackcard.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.CircularProgressIndicator
import com.venom.domain.model.AppTheme
import com.venom.domain.model.DashboardData
import com.venom.wordvibe.R
import com.venom.stackcard.ui.components.mastery.MiniProgressDashboard
import com.venom.ui.components.buttons.CloseButton
import com.venom.ui.components.buttons.GradientActionButton
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp
import com.venom.ui.components.dialogs.PlacementChoiceDialog
import com.venom.designsystem.theme.SynapseTheme
import com.venom.designsystem.theme.synapse
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    onStart: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onGoogleSignIn: () -> Unit = {},
    onOpenProgress: () -> Unit = {},
    onTakePlacement: () -> Unit = {},
    onDismissPlacementModal: () -> Unit = {},
    showPlacementModal: Boolean = false,
    isLoading: Boolean = false,
    isSignedIn: Boolean = false,
    userName: String? = null,
    userProgress: DashboardData? = null,
    modifier: Modifier = Modifier
) {
    var topic by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { delay(100); isVisible = true }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.95f,
        animationSpec = tween(400),
        label = "scale"
    )

    val displayName = userName?.takeIf { it.isNotBlank() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.adp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar(
            isSignedIn = isSignedIn,
            userName = displayName,
            isLoading = isLoading,
            onBack = onBack
        )

        if (userProgress != null) {
            DashboardSection(
                userProgress = userProgress,
                onOpenProgress = onOpenProgress,
                modifier = Modifier.weight(0.25f)
            )
        }

        Column(
            modifier = Modifier
                .weight(if (userProgress != null) 0.30f else 0.45f)
                .scale(scale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppLogo()
            Spacer(modifier = Modifier.height(20.adp))
            AppTitle()
        }

        Column(
            modifier = Modifier
                .weight(0.35f)
                .widthIn(max = 360.adp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.adp, Alignment.Top)
        ) {
GradientActionButton(
                text = stringResource(R.string.home_start_learning),
                onClick = { onStart(topic) },
                enabled = !isLoading,
                leadingIcon = R.drawable.icon_play,
                maxWidth = 360
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.adp),
                        strokeWidth = 2.adp
                    )
                }
            }
        }

        if (showPlacementModal) {
            PlacementChoiceDialog(
                onStartFromBeginning = {
                    onDismissPlacementModal()
                    onStart(topic)
                },
                onTakePlacementTest = {
                    onDismissPlacementModal()
                    onTakePlacement()
                },
                onDismiss = onDismissPlacementModal
            )
        }

        Spacer(modifier = Modifier.height(32.adp))
    }
}

@Composable
private fun DashboardSection(
    userProgress: DashboardData?,
    onOpenProgress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.adp))
            .clickable(onClick = onOpenProgress),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = userProgress != null,
            enter = fadeIn(tween(300)) + slideInVertically { -it },
            exit = fadeOut(tween(200)) + slideOutVertically { -it }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.adp)
            ) {
                MiniProgressDashboard(
                    data = userProgress ?: DashboardData(),
                    modifier = Modifier.widthIn(max = 360.adp)
                )
Text(
                    text = stringResource(R.string.home_tap_to_view_details),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    isSignedIn: Boolean,
    userName: String?,
    isLoading: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.adp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isLoading) {
            Box(modifier = Modifier.size(44.adp))
        } else {
            Box(modifier = Modifier.weight(1f))
        }

        if (!isLoading) {
            CloseButton(onBack)
        }
    }
}

@Composable
private fun TopicInput(
    topic: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = topic,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.adp, RoundedCornerShape(16.adp))
            .clip(RoundedCornerShape(16.adp)),
        placeholder = {
            Text(
                text = "Choose a topic (optional)",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
            )
        },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.icon_search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.adp)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(16.adp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
private fun AppLogo() {
    Box(modifier = Modifier.size(130.adp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(100.adp)
                .graphicsLayer { rotationZ = 6f }
                .shadow(
                    20.adp, RoundedCornerShape(28.adp),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(0.25f),
                    spotColor = MaterialTheme.colorScheme.secondary.copy(0.3f)
                )
                .background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)),
                    RoundedCornerShape(28.adp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.icon_graduation_cap),
                contentDescription = null,
                modifier = Modifier.size(50.adp),
                tint = Color.White
            )
        }

        Box(
            modifier = Modifier
                .size(40.adp)
                .align(Alignment.BottomEnd)
                .offset(x = 8.adp, y = 8.adp)
                .graphicsLayer { rotationZ = -15f }
                .shadow(10.adp, RoundedCornerShape(12.adp))
                .background(MaterialTheme.synapse.semantic.success, RoundedCornerShape(12.adp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Aa",
                fontSize = 16.asp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun AppTitle() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.adp)
    ) {
        Text(
            text = stringResource(R.string.home_brand_name),
            fontSize = 44.asp,
            letterSpacing = (-1).asp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = stringResource(R.string.home_tagline),
            fontSize = 15.asp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.asp
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun WelcomeScreenPreview() {
    SynapseTheme(darkTheme = true) {
        WelcomeScreen(isSignedIn = false, isLoading = false)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun WelcomeScreenSignedInPreview() {
    SynapseTheme(darkTheme = true) {
        WelcomeScreen(
            isSignedIn = false,
            userProgress = DashboardData(
                totalWordsLearned = 156, totalWordsMastered = 42, currentStreak = 7, todayXp = 85
            )
        )
    }
}

