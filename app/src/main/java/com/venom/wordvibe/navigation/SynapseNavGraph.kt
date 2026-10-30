package com.venom.wordvibe.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.venom.wordvibe.navigation.core.AnimatedNavHost
import com.venom.wordvibe.navigation.core.NavTransitions
import com.venom.wordvibe.navigation.core.navigateToStart
import com.venom.wordvibe.ui.screen.HomeRoute
import com.venom.wordvibe.ui.screen.ProgressRoute
import com.venom.wordvibe.ui.screen.SpellingQueueRoute
import com.venom.wordvibe.ui.screen.UnitsLessonRoute
import com.venom.wordvibe.ui.screen.OnboardingScreen
import com.venom.wordvibe.ui.viewmodel.RootViewModel
import com.venom.stackcard.ui.screen.WordMasteryScreen

/** Detail route for the unit preview lesson (topic = EDITORIAL_CHAIN name). */
private const val UNITS_LESSON_ROUTE = "wordvibe/units_lesson/{topic}"

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
            HomeRoute(
                onStartSession = { navController.navigateToStart(SynapseScreen.StackCard.route) },
                onPractice = { navController.navigateToStart(SynapseScreen.Write.route) },
                onUnitLesson = { topic -> navController.navigate("wordvibe/units_lesson/$topic") },
            )
        }

        composable(
            route = UNITS_LESSON_ROUTE,
            arguments = listOf(navArgument("topic") { type = NavType.StringType }),
            enterTransition = NavTransitions.fadeThroughEnter(),
            exitTransition = NavTransitions.fadeThroughExit(),
            popEnterTransition = NavTransitions.fadeThroughEnter(),
            popExitTransition = NavTransitions.fadeThroughExit(),
        ) { entry ->
            UnitsLessonRoute(
                topic = entry.arguments?.getString("topic").orEmpty(),
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
            route = SynapseScreen.Write.route,
            enterTransition = NavTransitions.fadeThroughEnter(),
            exitTransition = NavTransitions.fadeThroughExit(),
            popEnterTransition = NavTransitions.fadeThroughEnter(),
            popExitTransition = NavTransitions.fadeThroughExit(),
        ) {
            SpellingQueueRoute()
        }

        composable(
            route = SynapseScreen.Progress.route,
            enterTransition = NavTransitions.fadeThroughEnter(),
            exitTransition = NavTransitions.fadeThroughExit(),
            popEnterTransition = NavTransitions.fadeThroughEnter(),
            popExitTransition = NavTransitions.fadeThroughExit(),
        ) {
            ProgressRoute()
        }
    }
}
