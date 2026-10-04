package com.venom.stackcard.ui.screen

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venom.domain.model.QuizMode
import com.venom.lingospell.presentation.SpellingGameScreen
import com.venom.quiz.placement.PlacementScreen
import com.venom.quiz.ui.viewmodel.UnifiedQuizViewModel
import com.venom.stackcard.ui.components.flashcard.CardSwiperStack
import com.venom.stackcard.ui.components.insights.InsightsSheet
import com.venom.stackcard.ui.components.mastery.ActionBar
import com.venom.stackcard.ui.components.mastery.CardsProgressIndicator
import com.venom.stackcard.ui.components.mastery.HapticStrength
import com.venom.stackcard.ui.components.mastery.rememberHapticFeedback
import com.venom.stackcard.ui.viewmodel.WordMasteryEvent
import com.venom.stackcard.ui.viewmodel.WordMasteryUiState
import com.venom.stackcard.ui.viewmodel.WordMasteryViewModel
import com.venom.ui.components.common.adp
import com.venom.ui.components.common.asp
import com.venom.ui.viewmodel.TTSViewModel

@Composable
fun WordMasteryScreen(
    isGenerative: Boolean,
    onBack: () -> Unit = {},
    onGoogleSignIn: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: WordMasteryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPlacementScreen by remember { mutableStateOf(false) }
    var showProgress by remember { mutableStateOf(false) }

    // Handle system back button to return to WelcomeScreen
    BackHandler(
        enabled = uiState.visibleCards.isNotEmpty() || uiState.processedCardsCount > 0
    ) {
        viewModel.onEvent(WordMasteryEvent.BackToWelcome)
    }

    if (showPlacementScreen) {
        Dialog(
            onDismissRequest = { showPlacementScreen = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            PlacementScreen(
                onComplete = { result ->
                    showPlacementScreen = false
                    viewModel.onEvent(WordMasteryEvent.Initialize(isGenerative))
                },
                onBack = { showPlacementScreen = false }
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            showProgress -> {
                ProgressScreen(
                    viewModel = viewModel,
                    onBack = { showProgress = false }
                )
            }

            uiState.visibleCards.isEmpty() && uiState.processedCardsCount == 0 && uiState.error == null -> {
                WelcomeScreen(
                    onBack = onBack,
                    onGoogleSignIn = onGoogleSignIn,
                    isLoading = uiState.isLoading,
                    isSignedIn = uiState.isSignedIn,
                    userName = uiState.userName,
                    userProgress = uiState.userProgress,
                    onOpenProgress = { showProgress = true },
                    onTakePlacement = { showPlacementScreen = true },
                    showPlacementModal = uiState.showPlacementModal,
                    onDismissPlacementModal = { viewModel.onEvent(WordMasteryEvent.DismissPlacementModal) },
                    onStart = { topic ->
                        viewModel.onEvent(
                            WordMasteryEvent.Initialize(
                                isGenerative = isGenerative,
                                topic = topic
                            )
                        )
                    }
                )
            }

            uiState.error != null -> {
                ErrorView(
                    message = uiState.error ?: "Unknown error",
                    onRetry = { viewModel.onEvent(WordMasteryEvent.Initialize(uiState.isGenerativeMode)) }
                )
            }

            uiState.visibleCards.isEmpty() && uiState.processedCardsCount > 0 -> {
                SessionFinishedView(
                    sessionStats = uiState.sessionStats,
                    onBackToWelcome = { viewModel.onEvent(WordMasteryEvent.BackToWelcome) },
                    onViewProgress = { showProgress = true },
                    onExit = onBack
                )
            }

            else -> {
                MasteryContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }
}

@Composable
private fun MasteryContent(
    uiState: WordMasteryUiState,
    viewModel: WordMasteryViewModel,
    onEvent: (WordMasteryEvent) -> Unit
) {
    val haptic = rememberHapticFeedback()
    val currentHaptic by rememberUpdatedState(haptic)
    val ttsViewModel: TTSViewModel = hiltViewModel(LocalContext.current as ViewModelStoreOwner)
    val quizViewModel: UnifiedQuizViewModel =
        hiltViewModel(LocalContext.current as ComponentActivity)
    val quizState by quizViewModel.uiState.collectAsStateWithLifecycle()
    val quizInput = uiState.quizInput
    val currentQuizInput by rememberUpdatedState(quizInput)

    BackHandler(
        enabled = uiState.isSheetOpen && !(uiState.isPracticeMode && uiState.currentWord != null)
    ) {
        onEvent(WordMasteryEvent.CloseSheet)
    }

    val showQuiz = uiState.isPracticeMode && uiState.quizInput != null

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LaunchedEffect(quizInput) {
            quizInput?.let { input ->
                quizViewModel.start(QuizMode.Flashcard(input))
            }
        }

        LaunchedEffect(Unit) {
            quizViewModel.quizResult.collect { result ->
                onEvent(WordMasteryEvent.PracticeResult(result))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                // Keep the session ActionBar clear of the floating bottom bar.
                .padding(bottom = 104.adp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CardsProgressIndicator(
                currentIndex = uiState.graduatedCards,
                totalCards = uiState.initialCardCount,
                difficultyScore = uiState.currentWord?.difficultyScore ?: 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.adp, vertical = 12.adp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CardSwiperStack(
                    viewModel = viewModel,
                    onSpeak = { text -> ttsViewModel.toggle(text, null) },
                    onFlip = {
                        currentHaptic(HapticStrength.LIGHT)
                    },
                    onRememberWord = {
                        currentHaptic(HapticStrength.STRONG)
                    },
                    onForgotWord = {
                        currentHaptic(HapticStrength.LIGHT)
                    },
                    // Quiz params — decision is HERE 
                    /* showQuiz = showQuiz,
                     quizState = quizState,
                     onQuizSelect = quizViewModel::selectOption,
                     onQuizSkip = {
                         quizViewModel.skipFlashcard()
                         currentQuizInput?.let { input ->
                             onEvent(WordMasteryEvent.PracticeSkipped(input.wordId))
                         }
                     }*/
                )
            }

            ActionBar(
                isFlipped = uiState.isFlipped,
                onFlip = {
                    if (!uiState.isPracticeMode) {
                        onEvent(WordMasteryEvent.FlipCard)
                    }
                },
                onPractice = { onEvent(WordMasteryEvent.StartPractice) },
                onInfoClick = { onEvent(WordMasteryEvent.OpenSheet) },
                onKnown = {
                    uiState.currentWord?.let {
                        onEvent(WordMasteryEvent.SwipeCard(it, true))
                    }
                },
                onAgain = {
                    uiState.currentWord?.let {
                        onEvent(WordMasteryEvent.SwipeCard(it, false))
                    }
                },
                modifier = Modifier.padding(horizontal = 20.adp)
            )

            Spacer(modifier = Modifier.height(24.adp))
        }

        InsightsSheet(
            isOpen = uiState.isSheetOpen,
            word = uiState.currentWord,
            currentWordProgress = uiState.currentWordProgress,
            userCefrLevel = uiState.userCefrLevel,
            activeTab = uiState.activeTab,
            pinnedLanguage = uiState.pinnedLanguage,
            showPowerTip = uiState.showPowerTip,
            onClose = { onEvent(WordMasteryEvent.CloseSheet) },
            onTabChange = { tab -> onEvent(WordMasteryEvent.ChangeTab(tab)) },
            onPinLanguage = { lang -> onEvent(WordMasteryEvent.PinLanguage(lang)) },
            onTogglePowerTip = { onEvent(WordMasteryEvent.TogglePowerTip) }
        )

        if (uiState.isPracticeMode && uiState.currentWord != null) {
            Dialog(
                onDismissRequest = { onEvent(WordMasteryEvent.PracticeHandled) },
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                SpellingGameScreen(
                    customWord = uiState.currentWord,
                    onNavigateBack = { onEvent(WordMasteryEvent.PracticeHandled) }
                )
            }
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.adp),
            verticalArrangement = Arrangement.spacedBy(12.adp)
        ) {
            Text(
                text = stringResource(com.venom.wordvibe.R.string.error_something_wrong),
                fontSize = 20.asp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                fontSize = 14.asp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.adp))
            Button(onClick = onRetry) {
                Text(stringResource(com.venom.wordvibe.R.string.error_try_again))
            }
        }
    }
}