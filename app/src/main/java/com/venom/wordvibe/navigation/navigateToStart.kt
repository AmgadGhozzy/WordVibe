package com.venom.wordvibe.navigation

import androidx.navigation.NavHostController
import com.venom.ui.navigation.Screen

fun NavHostController.navigateToStart(screen: Screen) {
    val currentRoute = currentDestination?.route?.substringBefore("?")
    val targetRoute = screen.route

    if (currentRoute != targetRoute) {
        navigate(targetRoute) {
            popUpTo(graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}