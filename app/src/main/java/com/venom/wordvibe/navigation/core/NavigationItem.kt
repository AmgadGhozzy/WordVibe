package com.venom.wordvibe.navigation.core

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

interface NavigationItem {
    /** Route string — must match NavHost composable() route patterns exactly. */
    val route: String

    /** Drawable resource for the icon (filled / active state). */
    @get:DrawableRes
    val icon: Int

    /** Drawable resource for the icon (active state). */
    @get:DrawableRes
    val iconActive: Int

    /** String resource for the accessibility content description + label. */
    @get:StringRes
    val titleRes: Int

    /** Whether this item should appear in the bottom bar. */
    val showInBottomBar: Boolean
}
