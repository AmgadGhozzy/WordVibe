package com.venom.wordvibe.navigation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.venom.ui.navigation.Screen

class AppState(
    val navController: NavHostController
) {
    var currentBackStackEntry: NavBackStackEntry? by mutableStateOf(null)
    var showSettings by mutableStateOf(false)
    var showAbout by mutableStateOf(false)

    val currentRoute: String?
        get() = currentBackStackEntry?.destination?.route

    val currentScreen: Screen
        get() = Screen.fromRoute(currentRoute ?: Screen.Translation.route)

    val barConfig: BarConfig
        get() = BarConfig.forRoute(currentRoute)

    fun navigateToHistory(contentType: String) {
        navController.navigate(Screen.History.createRoute(contentType))
    }

    fun handleBackPress() {
        when {
            showSettings -> showSettings = false
            showAbout -> showAbout = false
            else -> navController.popBackStack()
        }
    }
}

@Immutable
data class BarConfig(
    val showTopBar: Boolean,
    val showBottomBar: Boolean,
) {
    companion object {
        private val BOTH = BarConfig(showTopBar = true, showBottomBar = true)
        private val NONE = BarConfig(showTopBar = false, showBottomBar = false)
        private val TOP_ONLY = BarConfig(showTopBar = true, showBottomBar = false)
        private val BOTTOM_ONLY = BarConfig(showTopBar = false, showBottomBar = true)

        fun forRoute(route: String?): BarConfig = when {
            route == null -> BOTH
            route == Screen.Onboarding.route -> NONE
            route == Screen.StackCard.route -> NONE
            route == Screen.Sentence.route -> NONE
            route.startsWith("quiz/test") -> NONE
            route.startsWith("quiz/exam") -> NONE
            route.startsWith("quiz/categories") -> NONE
            route.startsWith("quiz/tests") -> NONE
            route.startsWith("history") -> NONE
            route == Screen.Ocr.route -> TOP_ONLY
            route == Screen.Quiz.route -> BOTTOM_ONLY
            route == Screen.Quiz.Levels.route -> BOTTOM_ONLY
            route == Screen.Phrases.route -> BOTTOM_ONLY
            route == Screen.WordCraft.route -> BOTTOM_ONLY
            route == Screen.Quote.route -> BOTTOM_ONLY
            else -> BOTH
        }
    }
}