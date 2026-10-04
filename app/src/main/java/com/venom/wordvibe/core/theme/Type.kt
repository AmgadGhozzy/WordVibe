package com.venom.wordvibe.core.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import com.venom.domain.model.FontStyles

private val defaultType = Typography()

val Inter: FontFamily = FontFamily.Default
val InterSemiBold: FontFamily = FontFamily.Default
val InterBold: FontFamily = FontFamily.Default
val Cairo: FontFamily = FontFamily.Default
val Alexandria: FontFamily = FontFamily.Default
val Caveat: FontFamily = FontFamily.Default
val Roboto: FontFamily = FontFamily.Default
val Quicksand: FontFamily = FontFamily.Default
val JosefinSans: FontFamily = FontFamily.Default
val PlaypenSans: FontFamily = FontFamily.Default
val PlayfairDisplay: FontFamily = FontFamily.Default

/**
 * Safely loads a FontFamily with fallback to default if loading fails.
 */
@Composable
fun safeLoadFontFamily(fontStyle: FontStyles): FontFamily {
    return remember(fontStyle) {
        try {
            when (fontStyle) {
                FontStyles.INTER       -> Inter
                FontStyles.CAIRO       -> Cairo
                FontStyles.ALEXANDRIA  -> Alexandria
                FontStyles.CAVEAT      -> Caveat
                FontStyles.ROBOTO      -> Roboto
                FontStyles.QUICKSAND   -> Quicksand
                FontStyles.JOSEFIN_SANS -> JosefinSans
                FontStyles.PLAYPEN_SANS -> PlaypenSans
                else                   -> FontFamily.Default
            }
        } catch (_: Exception) {
            FontFamily.Default
        }
    }
}

// ── Typography builders ───────────────────────────────────────────────────────

/** Applies a single [fontFamily] to every typography role. */
fun getTypography(fontFamily: FontFamily) = defaultType.copy(
    displayLarge  = defaultType.displayLarge.copy(fontFamily  = fontFamily),
    displayMedium = defaultType.displayMedium.copy(fontFamily = fontFamily),
    displaySmall  = defaultType.displaySmall.copy(fontFamily  = fontFamily),
    headlineLarge  = defaultType.headlineLarge.copy(fontFamily  = fontFamily),
    headlineMedium = defaultType.headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall  = defaultType.headlineSmall.copy(fontFamily  = fontFamily),
    titleLarge  = defaultType.titleLarge.copy(fontFamily  = fontFamily),
    titleMedium = defaultType.titleMedium.copy(fontFamily = fontFamily),
    titleSmall  = defaultType.titleSmall.copy(fontFamily  = fontFamily),
    bodyLarge  = defaultType.bodyLarge.copy(fontFamily  = fontFamily),
    bodyMedium = defaultType.bodyMedium.copy(fontFamily = fontFamily),
    bodySmall  = defaultType.bodySmall.copy(fontFamily  = fontFamily),
    labelLarge  = defaultType.labelLarge.copy(fontFamily  = fontFamily),
    labelMedium = defaultType.labelMedium.copy(fontFamily = fontFamily),
    labelSmall  = defaultType.labelSmall.copy(fontFamily  = fontFamily),
)

/**
 * Builds [Typography] where:
 * - Arabic locale → [arabicFamily] (Cairo by default)
 * - All others    → [latinFamily]  (Inter by default)
 *
 * This is what [SynapseTheme] passes to [MaterialTheme].
 */
fun getTypographyForLocale(
    latinFamily: FontFamily,
    arabicFamily: FontFamily,
    isArabic: Boolean,
): Typography = getTypography(if (isArabic) arabicFamily else latinFamily)
