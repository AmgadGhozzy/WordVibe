package com.venom.wordvibe.navigation

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

@Immutable
object MotionTokens {
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val Standard = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val StandardDecelerate = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
    val StandardAccelerate = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)

    const val SHORT_2 = 150
    const val SHORT_3 = 200
    const val SHORT_4 = 250
    const val MEDIUM_1 = 250
    const val MEDIUM_2 = 300
    const val MEDIUM_3 = 350
    const val MEDIUM_4 = 400
    const val LONG_1 = 450
}

typealias NavEnter =
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition

typealias NavExit =
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition

@Immutable
object NavTransitions {

    private const val HORIZONTAL_OFFSET = 0.30f
    private const val VERTICAL_OFFSET = 0.25f
    private const val SCALE_FACTOR = 0.92f
    private const val FADE_IN_DELAY = 30
    private const val FADE_THROUGH_GAP = 90

    fun horizontalEnter(
        durationMs: Int = MotionTokens.MEDIUM_3,
        offsetFraction: Float = HORIZONTAL_OFFSET,
    ): NavEnter = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(durationMs, easing = MotionTokens.EmphasizedDecelerate),
            initialOffset = { (it * offsetFraction).toInt() },
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = MotionTokens.SHORT_4,
                delayMillis = FADE_IN_DELAY,
                easing = MotionTokens.StandardDecelerate,
            ),
        )
    }

    fun horizontalExit(
        durationMs: Int = MotionTokens.MEDIUM_2,
        offsetFraction: Float = HORIZONTAL_OFFSET,
    ): NavExit = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(durationMs, easing = MotionTokens.EmphasizedAccelerate),
            targetOffset = { (it * offsetFraction).toInt() },
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = MotionTokens.SHORT_3,
                easing = MotionTokens.StandardAccelerate,
            ),
        )
    }

    fun horizontalPopEnter(
        durationMs: Int = MotionTokens.MEDIUM_3,
        offsetFraction: Float = HORIZONTAL_OFFSET,
    ): NavEnter = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(durationMs, easing = MotionTokens.EmphasizedDecelerate),
            initialOffset = { (it * offsetFraction).toInt() },
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = MotionTokens.SHORT_4,
                delayMillis = FADE_IN_DELAY,
                easing = MotionTokens.StandardDecelerate,
            ),
        )
    }

    fun horizontalPopExit(
        durationMs: Int = MotionTokens.MEDIUM_2,
        offsetFraction: Float = HORIZONTAL_OFFSET,
    ): NavExit = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(durationMs, easing = MotionTokens.EmphasizedAccelerate),
            targetOffset = { (it * offsetFraction).toInt() },
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = MotionTokens.SHORT_3,
                easing = MotionTokens.StandardAccelerate,
            ),
        )
    }

    fun verticalEnter(
        durationMs: Int = MotionTokens.MEDIUM_3,
        offsetFraction: Float = VERTICAL_OFFSET,
    ): NavEnter = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Up,
            animationSpec = tween(durationMs, easing = MotionTokens.EmphasizedDecelerate),
            initialOffset = { (it * offsetFraction).toInt() },
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = MotionTokens.SHORT_4,
                easing = MotionTokens.StandardDecelerate,
            ),
        )
    }

    fun verticalExit(
        durationMs: Int = MotionTokens.MEDIUM_2,
    ): NavExit = {
        fadeOut(
            animationSpec = tween(durationMs, easing = MotionTokens.StandardAccelerate),
        ) + scaleOut(
            animationSpec = tween(durationMs, easing = MotionTokens.StandardAccelerate),
            targetScale = 0.96f,
        )
    }

    fun verticalPopEnter(
        durationMs: Int = MotionTokens.MEDIUM_1,
    ): NavEnter = {
        fadeIn(
            animationSpec = tween(durationMs, easing = MotionTokens.StandardDecelerate),
        ) + scaleIn(
            animationSpec = tween(durationMs, easing = MotionTokens.StandardDecelerate),
            initialScale = 0.96f,
        )
    }

    fun verticalPopExit(
        durationMs: Int = MotionTokens.MEDIUM_2,
        offsetFraction: Float = VERTICAL_OFFSET,
    ): NavExit = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Down,
            animationSpec = tween(durationMs, easing = MotionTokens.EmphasizedAccelerate),
            targetOffset = { (it * offsetFraction).toInt() },
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = MotionTokens.SHORT_3,
                easing = MotionTokens.StandardAccelerate,
            ),
        )
    }

    fun fadeThroughEnter(
        durationMs: Int = MotionTokens.MEDIUM_1,
        initialScale: Float = SCALE_FACTOR,
    ): NavEnter = {
        fadeIn(
            animationSpec = tween(
                durationMillis = durationMs,
                delayMillis = FADE_THROUGH_GAP,
                easing = MotionTokens.StandardDecelerate,
            ),
        ) + scaleIn(
            animationSpec = tween(
                durationMillis = durationMs,
                delayMillis = FADE_THROUGH_GAP,
                easing = MotionTokens.StandardDecelerate,
            ),
            initialScale = initialScale,
        )
    }

    fun fadeThroughExit(
        durationMs: Int = FADE_THROUGH_GAP,
    ): NavExit = {
        fadeOut(
            animationSpec = tween(durationMs, easing = MotionTokens.StandardAccelerate),
        )
    }

    val noneEnter: NavEnter = { EnterTransition.None }
    val noneExit: NavExit = { ExitTransition.None }
}

@Composable
fun AnimatedNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    navGraphBuilder: NavGraphBuilder.() -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = NavTransitions.horizontalEnter(),
        exitTransition = NavTransitions.horizontalExit(),
        popEnterTransition = NavTransitions.horizontalPopEnter(),
        popExitTransition = NavTransitions.horizontalPopExit(),
        builder = navGraphBuilder,
    )
}