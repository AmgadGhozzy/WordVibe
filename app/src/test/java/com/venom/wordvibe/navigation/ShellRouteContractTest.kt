package com.venom.wordvibe.navigation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Shell contract (Phase 1): onboarding + four real tabs (Home, Learn, Write,
 * Progress). Any placeholder/dead/duplicate destination re-entering the graph is
 * a regression this test catches by enumerating the closed route set.
 */
class ShellRouteContractTest {

    @Test
    fun `shell graph exposes onboarding plus the four real tabs`() {
        val routes = SynapseScreen::class.nestedClasses
            .mapNotNull { it.objectInstance as? SynapseScreen }
            .map { it.route }
            .toSet()

        assertEquals(
            setOf(
                "wordvibe/onboarding",
                "wordvibe/dashboard",
                "wordvibe/stackcard",
                "wordvibe/write",
                "wordvibe/progress",
            ),
            routes,
            "Placeholder/dead destinations must not be registered in the shell graph",
        )
    }

    @Test
    fun `bottom bar lists exactly the four real tabs`() {
        val barRoutes = SynapseNavigationItems.visibleItems.map { it.route }.toSet()

        assertEquals(
            setOf(
                "wordvibe/dashboard",
                "wordvibe/stackcard",
                "wordvibe/write",
                "wordvibe/progress",
            ),
            barRoutes,
            "Bottom bar must only carry the four real shell destinations",
        )
    }

    @Test
    fun `every tab has a distinct route, icon pair and Arabic label`() {
        val items = SynapseNavigationItems.visibleItems

        assertEquals(4, items.size, "exactly four tabs in Phase 1")
        assertEquals(items.size, items.map { it.route }.toSet().size, "routes are unique")
        assertEquals(items.size, items.map { it.titleRes }.toSet().size, "labels are unique")
        assertEquals(items.size, items.map { it.icon }.toSet().size, "icons are unique")
        // No tab may reuse the flashcard surface: the write tab is its own route.
        assertTrue(
            items.none { it.route == SynapseScreen.StackCard.route && it != SynapseNavigationItems.STACKCARD },
            "no duplicate surface entries",
        )
    }
}
