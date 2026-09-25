package com.venom.wordvibe.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.venom.wordvibe.navigation.core.AnimatedNavHost
import com.venom.wordvibe.navigation.core.NavTransitions
import com.venom.wordvibe.ui.screen.OnboardingScreen
import com.venom.wordvibe.ui.viewmodel.RootViewModel
import com.venom.stackcard.ui.screen.WelcomeScreen
import com.venom.stackcard.ui.screen.WordMasteryScreen

@Composable
fun WordVibeNavGraph(
    navController: NavHostController,
    onboardingDone: Boolean,
    rootViewModel: RootViewModel,
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
            OnboardingScreen(
                onStart = {
                    rootViewModel.completeOnboarding()
                    navController.navigate(SynapseScreen.Dashboard.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
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
            )
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
            )
        }
    }
}