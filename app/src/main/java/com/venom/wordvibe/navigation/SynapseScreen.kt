package com.venom.wordvibe.navigation

import androidx.compose.runtime.Immutable
import com.venom.wordvibe.R

sealed class SynapseScreen(
    val route: String,
    val titleRes: Int,
    val subtitleRes: Int = 0,
) {
    data object Onboarding : SynapseScreen(
        route = "wordvibe/onboarding",
        titleRes = R.string.synapse_screen_onboarding,
        subtitleRes = R.string.synapse_subtitle_onboarding,
    )

    data object Dashboard : SynapseScreen(
        route = "wordvibe/dashboard",
        titleRes = R.string.synapse_screen_dashboard,
    )

    data object StackCard : SynapseScreen(
        route = "wordvibe/stackcard",
        titleRes = R.string.synapse_screen_stackcard,
    )

    data object Lingospell : SynapseScreen(
        route = "wordvibe/lingospell",
        titleRes = R.string.synapse_screen_lingospell,
    )

    companion object {
        fun fromRoute(route: String?): SynapseScreen = when (route) {
            Dashboard.route -> Dashboard
            StackCard.route -> StackCard
            Lingospell.route -> Lingospell
            Onboarding.route -> Onboarding
            else -> Dashboard
        }
    }
}

@Immutable
data class BarConfig(
    val showTopBar: Boolean,
    val showBottomBar: Boolean,
) {
    companion object {
        private val BOTTOM = BarConfig(showTopBar = false, showBottomBar = true)
        private val NONE = BarConfig(showTopBar = false, showBottomBar = false)

        fun forRoute(route: String?): BarConfig {
            if (route == null) return BOTTOM
            return when (SynapseScreen.fromRoute(route)) {
                SynapseScreen.Onboarding -> NONE
                else -> BOTTOM
            }
        }
    }
}