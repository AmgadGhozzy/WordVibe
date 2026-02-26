package com.venom.wordvibe.ui

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.venom.data.model.TranslationProvider
import com.venom.wordvibe.navigation.AppState
import com.venom.wordvibe.navigation.MotionTokens
import com.venom.wordvibe.navigation.NavigationGraph
import com.venom.settings.presentation.screen.AboutBottomSheet
import com.venom.settings.presentation.screen.SettingsBottomSheet
import com.venom.ui.components.common.adp
import com.venom.ui.components.other.FloatingOrbs
import com.venom.ui.navigation.Screen
import com.venom.ui.screen.ContentType
import com.venom.ui.viewmodel.SettingsViewModel
import com.venom.ui.viewmodel.TranslateViewModel

private const val BAR_ENTER_MS = 250
private const val BAR_EXIT_MS = 200

private val TopBarEnter =
    slideInVertically(tween(BAR_ENTER_MS, easing = MotionTokens.EmphasizedDecelerate)) { -it } +
            fadeIn(tween(BAR_ENTER_MS - 50, delayMillis = 30, easing = MotionTokens.StandardDecelerate))

private val TopBarExit =
    slideOutVertically(tween(BAR_EXIT_MS, easing = MotionTokens.EmphasizedAccelerate)) { -it } +
            fadeOut(tween(BAR_EXIT_MS - 50, easing = MotionTokens.StandardAccelerate))

private val BottomBarEnter =
    slideInVertically(tween(BAR_ENTER_MS, easing = MotionTokens.EmphasizedDecelerate)) { it } +
            fadeIn(tween(BAR_ENTER_MS - 50, delayMillis = 30, easing = MotionTokens.StandardDecelerate))

private val BottomBarExit =
    slideOutVertically(tween(BAR_EXIT_MS, easing = MotionTokens.EmphasizedAccelerate)) { it } +
            fadeOut(tween(BAR_EXIT_MS - 50, easing = MotionTokens.StandardAccelerate))

private val PaddingSpec = tween<Dp>(BAR_ENTER_MS, easing = MotionTokens.Standard)

@Composable
fun LingoLensApp(
    startCamera: ((Uri?) -> Unit) -> Unit,
    imageSelector: ((Uri?) -> Unit) -> Unit,
    fileSelector: ((Uri?) -> Unit) -> Unit,
    onGoogleSignIn: () -> Unit = {},
) {
    val navController = rememberNavController()
    val appState = remember(navController) { AppState(navController) }.apply {
        currentBackStackEntry = navController.currentBackStackEntryAsState().value
    }

    LingoLensAppContent(
        appState = appState,
        startCamera = startCamera,
        imageSelector = imageSelector,
        fileSelector = fileSelector,
        onGoogleSignIn = onGoogleSignIn,
    )
}

@Composable
private fun LingoLensAppContent(
    appState: AppState,
    translateViewModel: TranslateViewModel = hiltViewModel(LocalActivity.current as ComponentActivity),
    settingsViewModel: SettingsViewModel = hiltViewModel(LocalActivity.current as ComponentActivity),
    startCamera: ((Uri?) -> Unit) -> Unit,
    imageSelector: ((Uri?) -> Unit) -> Unit,
    fileSelector: ((Uri?) -> Unit) -> Unit,
    onGoogleSignIn: () -> Unit,
) {
    BackHandler(enabled = appState.showSettings || appState.showAbout) {
        appState.handleBackPress()
    }

    val translationUiState by translateViewModel.uiState.collectAsStateWithLifecycle()
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val barConfig by remember { derivedStateOf { appState.barConfig } }

    val topPadding by animateDpAsState(
        targetValue = if (barConfig.showTopBar) 64.adp else 0.adp,
        animationSpec = PaddingSpec,
        label = "topPadding",
    )

    when {
        appState.showSettings -> SettingsBottomSheet(
            onDismiss = { appState.showSettings = false },
            navController = appState.navController,
        )
        appState.showAbout -> AboutBottomSheet(
            onDismiss = { appState.showAbout = false },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        if (settingsUiState.orbPrefs.enabled) {
            FloatingOrbs(
                enableFloatingAnimation = settingsUiState.orbPrefs.enableFloatingAnimation,
                enableScaleAnimation = settingsUiState.orbPrefs.enableScaleAnimation,
                enableAlphaAnimation = settingsUiState.orbPrefs.enableAlphaAnimation,
                animationDuration = (10000 / settingsUiState.orbPrefs.animationSpeed).toInt(),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = topPadding),
        ) {
            NavigationGraph(
                navController = appState.navController,
                startCamera = startCamera,
                imageSelector = imageSelector,
                fileSelector = fileSelector,
                onGoogleSignIn = onGoogleSignIn,
            )
        }

        AnimatedVisibility(
            visible = barConfig.showTopBar,
            enter = TopBarEnter,
            exit = TopBarExit,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
            label = "TopBar",
        ) {
            LingoLensTopBar(
                currentScreen = appState.currentScreen,
                onNavigateBack = { appState.navController.popBackStack() },
                onBookmarkClick = {
                    appState.navigateToHistory(appState.currentScreen.toContentType().name)
                },
                onSettingsClick = { appState.showSettings = true },
                onAboutClick = { appState.showAbout = true },
                showProviderSelector = appState.currentScreen == Screen.Translation,
                selectedProvider = translationUiState.selectedProvider,
                availableProviders = if (appState.currentScreen == Screen.Translation)
                    TranslationProvider.ALL else emptyList(),
                onProviderSelected = translateViewModel::updateProvider,
            )
        }

        AnimatedVisibility(
            visible = barConfig.showBottomBar,
            enter = BottomBarEnter,
            exit = BottomBarExit,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            label = "BottomBar",
        ) {
            LingoLensBottomBar(
                navController = appState.navController,
                currentScreen = appState.currentScreen,
            )
        }
    }
}

private fun Screen.toContentType() = when (this) {
    Screen.Translation -> ContentType.TRANSLATION
    Screen.Ocr -> ContentType.OCR
    Screen.Phrases -> ContentType.PHRASEBOOK
    Screen.Dialog -> ContentType.DIALOG
    Screen.StackCard -> ContentType.STACKCARD
    else -> ContentType.TRANSLATION
}