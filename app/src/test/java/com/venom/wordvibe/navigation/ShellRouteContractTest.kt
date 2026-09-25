package com.venom.wordvibe.navigation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Ticket 06: the shell exposes exactly the MVP destinations. Any placeholder or dead
 * destination re-introduced into the graph (via SynapseScreen or the bottom bar) is a
 * regression this test catches by enumerating the closed set of routes.
 */
class ShellRouteContractTest {

    @Test
    fun `shell graph exposes only the MVP destinations`() {
        val routes = SynapseScreen::class.nestedClasses
            .mapNotNull { it.objectInstance as? SynapseScreen }
            .map { it.route }
            .toSet()

        assertEquals(
            setOf(
                "wordvibe/onboarding",
                "wordvibe/dashboard",
                "wordvibe/stackcard",
                "wordvibe/lingospell",
            ),
            routes,
            "Placeholder/dead destinations must not be registered in the shell graph",
        )
    }

    @Test
    fun `bottom bar lists exactly Home, Flashcards and Spelling`() {
        val barRoutes = SynapseNavigationItems.visibleItems.map { it.route }.toSet()

        assertEquals(
            setOf(
                "wordvibe/dashboard",
                "wordvibe/stackcard",
                "wordvibe/lingospell",
            ),
            barRoutes,
            "Bottom bar must only carry the three real shell destinations",
        )
    }
}