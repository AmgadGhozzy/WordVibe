package com.venom.wordvibe.navigation.core

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController

@Stable
abstract class BaseAppState(
    val navController: NavHostController,
) {

    // ── Back-stack tracking ─────────────────────────────────────────

    private var _currentBackStackEntry: NavBackStackEntry? by mutableStateOf(null)

    val currentBackStackEntry: NavBackStackEntry?
        get() = _currentBackStackEntry

    /**
     * Call this ONLY from the factory composable `rememberXxxAppState()`.
     * Pipes NavController's `currentBackStackEntryAsState()` into this
     * plain class without requiring @Composable context here.
     */
    fun updateCurrentEntry(entry: NavBackStackEntry?) {
        _currentBackStackEntry = entry
    }

    val currentRoute: String?
        get() = _currentBackStackEntry?.destination?.route

    // ── Primitive navigation ────────────────────────────────────────

    /**
     * Raw navigate — prefer app-specific helpers (navigateToTab, etc.)
     * for semantically correct back-stack behaviour.
     */
    fun navigateTo(route: String) = navController.navigate(route)

    /** @return true if the back stack was popped, false if it was already empty */
    fun navigateBack(): Boolean = navController.popBackStack()
}
