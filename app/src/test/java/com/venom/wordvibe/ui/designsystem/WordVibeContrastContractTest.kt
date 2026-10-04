package com.venom.wordvibe.ui.designsystem

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.pow

/**
 * Phase 1 closure contract: every text/background pair the product actually renders
 * meets WCAG AA (>= 4.5:1). Computed from the real palette values, so any future
 * color change that drops below AA fails here instead of shipping.
 */
class WordVibeContrastContractTest {

    private fun channel(c: Float): Double {
        val v = c.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(c: Color): Double =
        0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    private fun ratio(fg: Color, bg: Color): Double {
        val (hi, lo) = listOf(luminance(fg), luminance(bg)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun assertAa(name: String, fg: Color, bg: Color) {
        val r = ratio(fg, bg)
        assertTrue(
            r >= 4.5,
            "$name contrast ${"%.2f".format(r)}:1 is below WCAG AA (4.5:1)"
        )
    }

    @Test
    fun `light theme word states and core text meet WCAG AA`() {
        val s = wordStateColors(dark = false)
        val c = WordVibeLightColors
        assertAa("light NEW", s.isNew, s.isNewContainer)
        assertAa("light LEARNING", s.isLearning, s.isLearningContainer)
        assertAa("light KNOWN", s.isKnown, s.isKnownContainer)
        assertAa("light MASTERED", s.isMastered, s.isMasteredContainer)
        assertAa("light DUE", s.isDue, s.isDueContainer)
        assertAa("light onPrimary/primary", c.onPrimary, c.primary)
        assertAa("light onSurface/background", c.onSurface, c.background)
        assertAa("light onSurfaceVariant/background", c.onSurfaceVariant, c.background)
    }

    @Test
    fun `dark theme word states and core text meet WCAG AA`() {
        val s = wordStateColors(dark = true)
        val c = WordVibeDarkColors
        assertAa("dark NEW", s.isNew, s.isNewContainer)
        assertAa("dark LEARNING", s.isLearning, s.isLearningContainer)
        assertAa("dark KNOWN", s.isKnown, s.isKnownContainer)
        assertAa("dark MASTERED", s.isMastered, s.isMasteredContainer)
        assertAa("dark DUE", s.isDue, s.isDueContainer)
        assertAa("dark onPrimary/primary", c.onPrimary, c.primary)
        assertAa("dark onSurface/background", c.onSurface, c.background)
        assertAa("dark onSurfaceVariant/background", c.onSurfaceVariant, c.background)
    }
}
