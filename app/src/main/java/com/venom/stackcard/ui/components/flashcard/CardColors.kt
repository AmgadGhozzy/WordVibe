package com.venom.stackcard.ui.components.flashcard

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.venom.designsystem.theme.Emerald500
import com.venom.designsystem.theme.Red500
import kotlin.math.abs

@Stable
object CardColors {
    val NeutralBorder = Color(0x33808080)
    val NeutralBorderDark = Color(0x33FFFFFF)

    val RememberBorderMin = Emerald500.copy(alpha = 0.3f)
    val RememberBorderMax = Emerald500.copy(alpha = 0.7f)

    val ForgotBorderMin = Red500.copy(alpha = 0.3f)
    val ForgotBorderMax = Red500.copy(alpha = 0.7f)

    const val PROGRESS_THRESHOLD = 0.3f

    fun getBorderColor(progress: Float, isDarkTheme: Boolean = false): Color {
        val absProgress = abs(progress)

        return when {
            absProgress < PROGRESS_THRESHOLD -> {
                if (isDarkTheme) NeutralBorderDark else NeutralBorder
            }

            progress > 0 -> {
                val t = ((absProgress - PROGRESS_THRESHOLD) / (1f - PROGRESS_THRESHOLD))
                    .coerceIn(0f, 1f)
                lerp(RememberBorderMin, RememberBorderMax, t)
            }

            else -> {
                val t = ((absProgress - PROGRESS_THRESHOLD) / (1f - PROGRESS_THRESHOLD))
                    .coerceIn(0f, 1f)
                lerp(ForgotBorderMin, ForgotBorderMax, t)
            }
        }
    }
}
