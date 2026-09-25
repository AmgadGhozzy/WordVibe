package com.venom.wordvibe.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venom.wordvibe.navigation.AppState
import com.venom.wordvibe.navigation.BarConfig
import com.venom.wordvibe.navigation.SynapseNavigationItems
import com.venom.wordvibe.navigation.WordVibeNavGraph
import com.venom.wordvibe.navigation.core.BottomBar
import com.venom.wordvibe.navigation.core.MotionTokens
import com.venom.wordvibe.navigation.rememberSynapseAppState
import com.venom.wordvibe.ui.viewmodel.RootViewModel

@Composable
fun WordVibeApp(
    rootViewModel: RootViewModel,
) {
    val appState = rememberSynapseAppState()

    WordVibeShell(
        appState = appState,
        rootViewModel = rootViewModel,
    )
}

private val BarEnter =
    slideInVertically(tween(280, easing = MotionTokens.EmphasizedDecelerate)) { it } +
            fadeIn(tween(230, easing = MotionTokens.StandardDecelerate))

private val BarExit =
    slideOutVertically(tween(220, easing = MotionTokens.EmphasizedAccelerate)) { it } +
            fadeOut(tween(180, easing = MotionTokens.StandardAccelerate))

@Composable
private fun WordVibeShell(
    appState: AppState,
    rootViewModel: RootViewModel,
) {
    val onboardingDone by rootViewModel.onboardingDone.collectAsStateWithLifecycle()
    val onboardingResolved = !rootViewModel.isLoadingOnboardingState
    val barConfig: BarConfig = appState.barConfig
    val routeReady = appState.currentRoute != null

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (onboardingResolved) {
                WordVibeNavGraph(
                    navController = appState.navController,
                    onboardingDone = onboardingDone,
                    rootViewModel = rootViewModel,
                )
            }

            AnimatedVisibility(
                visible = onboardingResolved && routeReady && barConfig.showBottomBar,
                enter = BarEnter,
                exit = BarExit,
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                BottomBar(
                    items = SynapseNavigationItems.visibleItems,
                    navController = appState.navController,
                    currentRoute = appState.currentRoute,
                )
            }
        }
    }
}