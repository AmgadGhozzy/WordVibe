package com.venom.wordvibe.navigation.core

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

// ──────────────────────────────────────────────────────────────────────
// MOTION TOKENS — M3 motion system easing curves
// ──────────────────────────────────────────────────────────────────────

@Immutable
object MotionTokens {
    // M3 easing curves
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f,  0.1f,  1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f,  0.0f,  0.8f,  0.15f)
    val Emphasized           = CubicBezierEasing(0.2f,  0.0f,  0.0f,  1.0f)
    val Standard             = CubicBezierEasing(0.2f,  0.0f,  0.0f,  1.0f)
    val StandardDecelerate   = CubicBezierEasing(0.0f,  0.0f,  0.0f,  1.0f)
    val StandardAccelerate   = CubicBezierEasing(0.3f,  0.0f,  1.0f,  1.0f)

    // Duration steps
    const val SHORT_2  = 150
    const val SHORT_3  = 200
    const val SHORT_4  = 250
    const val MEDIUM_1 = 250
    const val MEDIUM_2 = 300
    const val MEDIUM_3 = 350
    const val MEDIUM_4 = 400
    const val LONG_1   = 450
    const val LONG_2   = 500
}

// ──────────────────────────────────────────────────────────────────────
// TYPE ALIASES
// ──────────────────────────────────────────────────────────────────────

typealias NavEnter = AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
typealias NavExit  = AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition

// ──────────────────────────────────────────────────────────────────────
// NAV TRANSITIONS
// ──────────────────────────────────────────────────────────────────────

@Immutable
object NavTransitions {

    private const val HORIZONTAL_OFFSET  = 0.30f
    private const val VERTICAL_OFFSET    = 0.28f
    private const val SCALE_ENTER        = 0.94f   // subtle zoom-in on enter
    private const val SCALE_EXIT         = 0.96f   // subtle zoom-out on exit
    private const val FADE_IN_DELAY      = 35
    private const val FADE_THROUGH_GAP   = 80

    // ── Horizontal — peer-level push/pop (default for most screens) ──

    fun horizontalEnter(
        durationMs:     Int   = MotionTokens.MEDIUM_3,
        offsetFraction: Float = HORIZONTAL_OFFSET,
    ): NavEnter = {
        slideIntoContainer(
            towards       = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(durationMs, easing = MotionTokens.EmphasizedDecelerate),
            initialOffset = { (it * offsetFraction).toInt() },
        ) + fadeIn(tween(MotionTokens.SHORT_4, delayMillis = FADE_IN_DELAY, easing = MotionTokens.StandardDecelerate))
    }

    fun horizontalExit(
        durationMs:     Int   = MotionTokens.MEDIUM_2,
        offsetFraction: Float = HORIZONTAL_OFFSET,
    ): NavExit = {
        slideOutOfContainer(
            towards       = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(durationMs, easing = MotionTokens.EmphasizedAccelerate),
            targetOffset  = { (it * offsetFraction).toInt() },
        ) + fadeOut(tween(MotionTokens.SHORT_3, easing = MotionTokens.StandardAccelerate))
    }

    fun horizontalPopEnter(
        durationMs:     Int   = MotionTokens.MEDIUM_3,
        offsetFraction: Float = HORIZONTAL_OFFSET,
    ): NavEnter = {
        slideIntoContainer(
            towards       = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(durationMs, easing = MotionTokens.EmphasizedDecelerate),
            initialOffset = { (it * offsetFraction).toInt() },
        ) + fadeIn(tween(MotionTokens.SHORT_4, delayMillis = FADE_IN_DELAY, easing = MotionTokens.StandardDecelerate))
    }

    fun horizontalPopExit(
        durationMs:     Int   = MotionTokens.MEDIUM_2,
        offsetFraction: Float = HORIZONTAL_OFFSET,
    ): NavExit = {
        slideOutOfContainer(
            towards       = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(durationMs, easing = MotionTokens.EmphasizedAccelerate),
            targetOffset  = { (it * offsetFraction).toInt() },
        ) + fadeOut(tween(MotionTokens.SHORT_3, easing = MotionTokens.StandardAccelerate))
    }

    // ── Vertical — modal sheet slide-up ─────────────────────────────

    fun verticalEnter(
        durationMs:     Int   = MotionTokens.MEDIUM_4,
        offsetFraction: Float = VERTICAL_OFFSET,
    ): NavEnter = {
        slideIntoContainer(
            towards       = AnimatedContentTransitionScope.SlideDirection.Up,
            animationSpec = tween(durationMs, easing = MotionTokens.EmphasizedDecelerate),
            initialOffset = { (it * offsetFraction).toInt() },
        ) + fadeIn(tween(MotionTokens.SHORT_4, easing = MotionTokens.StandardDecelerate))
    }

    fun verticalExit(durationMs: Int = MotionTokens.MEDIUM_2): NavExit = {
        fadeOut(tween(durationMs, easing = MotionTokens.StandardAccelerate)) +
                scaleOut(
                    animationSpec = tween(durationMs, easing = MotionTokens.StandardAccelerate),
                    targetScale   = SCALE_EXIT,
                )
    }

    fun verticalPopEnter(durationMs: Int = MotionTokens.MEDIUM_2): NavEnter = {
        fadeIn(tween(durationMs, easing = MotionTokens.StandardDecelerate)) +
                scaleIn(
                    animationSpec = tween(durationMs, easing = MotionTokens.StandardDecelerate),
                    initialScale  = SCALE_EXIT,
                )
    }

    fun verticalPopExit(
        durationMs:     Int   = MotionTokens.MEDIUM_3,
        offsetFraction: Float = VERTICAL_OFFSET,
    ): NavExit = {
        slideOutOfContainer(
            towards       = AnimatedContentTransitionScope.SlideDirection.Down,
            animationSpec = tween(durationMs, easing = MotionTokens.EmphasizedAccelerate),
            targetOffset  = { (it * offsetFraction).toInt() },
        ) + fadeOut(tween(MotionTokens.SHORT_3, easing = MotionTokens.StandardAccelerate))
    }

    // ── Fade-through — bottom nav tab switches ───────────────────────
    // Two-phase: outgoing fades out quickly, incoming fades+scales in
    // with a slight delay so both are never fully opaque at once.

    fun fadeThroughEnter(
        durationMs:   Int   = MotionTokens.MEDIUM_2,
        initialScale: Float = SCALE_ENTER,
    ): NavEnter = {
        fadeIn(
            tween(durationMs, delayMillis = FADE_THROUGH_GAP, easing = MotionTokens.StandardDecelerate)
        ) + scaleIn(
            tween(durationMs + FADE_THROUGH_GAP, easing = MotionTokens.Emphasized),
            initialScale = initialScale,
        )
    }

    fun fadeThroughExit(durationMs: Int = FADE_THROUGH_GAP): NavExit = {
        fadeOut(tween(durationMs, easing = MotionTokens.StandardAccelerate))
    }

    // ── None — destinations that manage their own enter animation ────
    val noneEnter: NavEnter = { EnterTransition.None }
    val noneExit:  NavExit  = { ExitTransition.None }
}

// ──────────────────────────────────────────────────────────────────────
// ANIMATED NAV HOST
// ──────────────────────────────────────────────────────────────────────

/**
 * Drop-in replacement for [NavHost] with M3 motion baked in as defaults.
 *
 * Default transitions (applied to every composable that does NOT declare
 * its own): horizontal push/pop — the correct pattern for peer-level
 * navigation. Screens that need a different motion (modals, tabs,
 * onboarding) override at the composable() call site in the nav graph.
 *
 * This removes all the boilerplate enterTransition/exitTransition params
 * from every tabComposable() call in SynapseNavGraph — only non-default
 * motions need to be declared explicitly.
 */
@Composable
fun AnimatedNavHost(
    navController:    NavHostController,
    startDestination: String,
    modifier:         Modifier = Modifier,
    route:            String?  = null,
    builder:          NavGraphBuilder.() -> Unit,
) {
    NavHost(
        navController      = navController,
        startDestination   = startDestination,
        modifier           = modifier,
        route              = route,
        builder            = builder,
        // ✅ NavHost-level defaults — any composable() that omits its own
        //    transition params will fall back to these automatically.
        enterTransition    = NavTransitions.horizontalEnter(),
        exitTransition     = NavTransitions.horizontalExit(),
        popEnterTransition = NavTransitions.horizontalPopEnter(),
        popExitTransition  = NavTransitions.horizontalPopExit(),
    )
}