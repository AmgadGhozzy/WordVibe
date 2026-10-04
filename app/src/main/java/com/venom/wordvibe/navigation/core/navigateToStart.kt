package com.venom.wordvibe.navigation.core

import androidx.navigation.NavHostController

/**
 * Navigate to a bottom-nav destination using MD3-correct back-stack behaviour.
 *
 * MIGRATION from the old signature `navigateToStart(screen: Screen)`:
 *   The old version accepted com.venom.wordvibe.navigation.Screen directly,
 *   making it unusable from Synapse (io.synapse.ai.navigation.Screen).
 *   Accepting `route: String` makes this extension app-agnostic.
 *
 * WHY route String (not NavigationItem):
 *   Avoids importing NavigationItem just for navigation logic.
 *   NavigationItem is a data contract; NavHostController is infrastructure.
 *   Keeping the two separate makes each independently testable.
 *
 * CALL SITES:
 *   SynapseBottomBar2:  navController.navigateToStart(item.route)
 *   SynapseShell / bar:   navController.navigateToStart(item.route)
 *
 * BEHAVIOUR:
 *   - No-op if already on the target route (prevents redundant recomposition)
 *   - Pops back stack to graph start with saveState = true
 *   - launchSingleTop + restoreState for correct MD3 tab semantics
 */
fun NavHostController.navigateToStart(route: String) {
    val currentRoute = currentDestination?.route?.substringBefore("?")

    if (currentRoute != route) {
        navigate(route) {
            popUpTo(graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState    = true
        }
    }
}