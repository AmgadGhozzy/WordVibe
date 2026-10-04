package com.venom.wordvibe.navigation

import com.venom.wordvibe.R
import com.venom.wordvibe.navigation.core.NavigationItem

enum class SynapseNavigationItems(
    override val route: String,
    override val icon: Int,
    override val iconActive: Int,
    override val titleRes: Int,
    override val showInBottomBar: Boolean = true,
) : NavigationItem {

    DASHBOARD(
        route    = SynapseScreen.Dashboard.route,
        icon     = R.drawable.ic_home,
        iconActive = R.drawable.ic_home_fill,
        titleRes = R.string.synapse_nav_home,
    ),
    STACKCARD(
        route    = SynapseScreen.StackCard.route,
        icon     = R.drawable.ic_books,
        iconActive = R.drawable.ic_books_fill,
        titleRes = R.string.nav_learn,
    ),
    WRITE(
        route    = SynapseScreen.Write.route,
        icon     = R.drawable.ic_edit,
        iconActive = R.drawable.ic_edit_fill,
        titleRes = R.string.nav_write,
    ),
    PROGRESS(
        route    = SynapseScreen.Progress.route,
        icon     = R.drawable.ic_bar_chart,
        iconActive = R.drawable.ic_bar_chart_fill,
        titleRes = R.string.nav_progress,
    );

    companion object {
        val visibleItems: List<SynapseNavigationItems> =
            entries.filter { it.showInBottomBar }
    }
}
