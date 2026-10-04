package com.venom.wordvibe.ui.designsystem

import android.os.Build
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.venom.wordvibe.R

/**
 * Cairo (variable) covers Arabic and Latin from a single file. Each weight is the
 * same font resource with an explicit variation weight — no faux bold, no subset
 * gaps. Variable settings require API 26+; below that the same file renders its
 * default instance with platform weight synthesis (minSdk 24 path).
 */
private fun cairoFontFamily(): FontFamily {
    val weights = listOf(
        FontWeight.Normal to 400,
        FontWeight.Medium to 500,
        FontWeight.SemiBold to 600,
        FontWeight.Bold to 700,
        FontWeight.ExtraBold to 800,
    )
    val fonts = weights.map { (weight, value) ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Font(
                resId = R.font.cairo_variable,
                weight = weight,
                variationSettings = FontVariation.Settings(FontVariation.weight(value)),
            )
        } else {
            Font(resId = R.font.cairo_variable, weight = weight)
        }
    }
    return FontFamily(fonts)
}

private val CairoVariable = cairoFontFamily()

val WordVibeTypography: Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = CairoVariable,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 46.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = CairoVariable,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 42.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = CairoVariable,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 38.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = CairoVariable,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 34.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = CairoVariable,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 30.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = CairoVariable,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = CairoVariable,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = CairoVariable,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.2.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = CairoVariable,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 24.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = CairoVariable,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 19.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = CairoVariable,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = CairoVariable,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = CairoVariable,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)
