package com.venom.wordvibe.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.venom.wordvibe.navigation.core.AnimatedNavHost
import com.venom.wordvibe.navigation.core.NavTransitions
import com.venom.wordvibe.ui.screen.PlaceholderScreen
import com.venom.wordvibe.ui.viewmodel.RootViewModel
import com.venom.stackcard.ui.screen.WelcomeScreen
import com.venom.stackcard.ui.screen.WordMasteryScreen

@Composable
fun WordVibeNavGraph(
    navController: NavHostController,
    onboardingDone: Boolean,
    rootViewModel: RootViewModel,
    startCamera: ((Uri?) -> Unit) -> Unit,
    imageSelector: ((Uri?) -> Unit) -> Unit,
    fileSelector: ((Uri?) -> Unit) -> Unit,
    onGoogleSignIn: () -> Unit = {},
) {
    val startDestination = if (onboardingDone) SynapseScreen.Dashboard.route else SynapseScreen.Onboarding.route

    AnimatedNavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(
            route = SynapseScreen.Onboarding.route,
            enterTransition = NavTransitions.fadeThroughEnter(),
            exitTransition = NavTransitions.fadeThroughExit(),
            popEnterTransition = NavTransitions.fadeThroughEnter(),
            popExitTransition = NavTransitions.fadeThroughExit(),
        ) {
            PlaceholderScreen("Onboarding")
        }

        composable(
            route = SynapseScreen.Dashboard.route,
            enterTransition = NavTransitions.fadeThroughEnter(),
            exitTransition = NavTransitions.fadeThroughExit(),
            popEnterTransition = NavTransitions.fadeThroughEnter(),
            popExitTransition = NavTransitions.fadeThroughExit(),
        ) {
            WelcomeScreen(
                onStart = { navController.navigate(SynapseScreen.StackCard.route) },
                onBack = { navController.popBackStack() },
                onGoogleSignIn = onGoogleSignIn,
                onOpenProgress = { navController.navigate(SynapseScreen.Stats.route) },
                onTakePlacement = { navController.navigate(SynapseScreen.Quiz.route) },
            )
        }

        composable(
            route = SynapseScreen.Library.route,
            enterTransition = NavTransitions.fadeThroughEnter(),
            exitTransition = NavTransitions.fadeThroughExit(),
            popEnterTransition = NavTransitions.fadeThroughEnter(),
            popExitTransition = NavTransitions.fadeThroughExit(),
        ) {
            PlaceholderScreen("Library")
        }

        composable(
            route = SynapseScreen.Stats.route,
            enterTransition = NavTransitions.fadeThroughEnter(),
            exitTransition = NavTransitions.fadeThroughExit(),
            popEnterTransition = NavTransitions.fadeThroughEnter(),
            popExitTransition = NavTransitions.fadeThroughExit(),
        ) {
            PlaceholderScreen("Stats")
        }

        composable(
            route = "${SynapseScreen.Translation.baseRoute}?${SynapseScreen.Translation.ARG_TEXT}={${SynapseScreen.Translation.ARG_TEXT}}",
            arguments = listOf(
                navArgument(SynapseScreen.Translation.ARG_TEXT) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            PlaceholderScreen("Translation")
        }

        composable(
            route = SynapseScreen.StackCard.route,
            enterTransition = NavTransitions.fadeThroughEnter(),
            exitTransition = NavTransitions.fadeThroughExit(),
            popEnterTransition = NavTransitions.fadeThroughEnter(),
            popExitTransition = NavTransitions.fadeThroughExit(),
        ) {
            WordMasteryScreen(
                isGenerative = false,
                onBack = { navController.popBackStack() },
                onGoogleSignIn = onGoogleSignIn,
            )
        }

        composable(
            route = SynapseScreen.Lingospell.route,
            enterTransition = NavTransitions.fadeThroughEnter(),
            exitTransition = NavTransitions.fadeThroughExit(),
            popEnterTransition = NavTransitions.fadeThroughEnter(),
            popExitTransition = NavTransitions.fadeThroughExit(),
        ) {
            WordMasteryScreen(
                isGenerative = false,
                onBack = { navController.popBackStack() },
                onGoogleSignIn = onGoogleSignIn,
            )
        }

        composable(
            route = SynapseScreen.Ocr.route,
            enterTransition = NavTransitions.verticalEnter(),
            exitTransition = NavTransitions.verticalExit(),
            popEnterTransition = NavTransitions.verticalPopEnter(),
            popExitTransition = NavTransitions.verticalPopExit(),
        ) {
            PlaceholderScreen("OCR")
        }

        composable(route = SynapseScreen.Phrases.route) {
            PlaceholderScreen("Phrases")
        }

        composable(
            route = SynapseScreen.Dialog.route,
            enterTransition = NavTransitions.verticalEnter(),
            exitTransition = NavTransitions.verticalExit(),
            popEnterTransition = NavTransitions.verticalPopEnter(),
            popExitTransition = NavTransitions.verticalPopExit(),
        ) {
            PlaceholderScreen("Dialog")
        }

        composable(
            route = SynapseScreen.Quiz.route,
        ) {
            PlaceholderScreen("Quiz")
        }

        composable(
            route = SynapseScreen.Profile.route,
            enterTransition = NavTransitions.fadeThroughEnter(),
            exitTransition = NavTransitions.fadeThroughExit(),
            popEnterTransition = NavTransitions.fadeThroughEnter(),
            popExitTransition = NavTransitions.fadeThroughExit(),
        ) {
            PlaceholderScreen("Profile")
        }
    }
}
