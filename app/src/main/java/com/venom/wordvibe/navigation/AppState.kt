package com.venom.wordvibe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.venom.wordvibe.navigation.core.BaseAppState

class AppState(
    navController: NavHostController,
) : BaseAppState(navController) {

    val currentScreen: SynapseScreen
        get() = SynapseScreen.fromRoute(currentRoute)

    val barConfig: BarConfig
        get() = BarConfig.forRoute(currentRoute)
}

@Composable
fun rememberSynapseAppState(
    navController: NavHostController = rememberNavController(),
): AppState {
    val appState = remember(navController) { AppState(navController) }
    val entry by navController.currentBackStackEntryAsState()
    appState.updateCurrentEntry(entry)
    return appState
}