package com.venom.data.model

import com.venom.domain.model.AppTheme
import com.venom.domain.model.FontStyles
import com.venom.domain.model.PaletteStyle
import kotlinx.serialization.Serializable

@Serializable
data class SettingsPreferences(
    val appLanguage: LanguageItem = LANGUAGES_LIST[0],
    val nativeLanguage: LanguageItem = LANGUAGES_LIST[0],
    val targetLanguage: LanguageItem = LANGUAGES_LIST[1],
    val selectedProvider: TranslationProvider = TranslationProvider.GOOGLE,
    val speechRate: Float = 1.2f,
    val themePrefs: ThemePreference = ThemePreference(),
    val personalPrefs: PersonalPreference = PersonalPreference(),
    val splashPrefs: SplashPreferences = SplashPreferences(),
    val orbPrefs: OrbPreferences = OrbPreferences(),
    val autoPronunciation: Boolean = true
)

@Serializable
data class ThemePreference(
    val appTheme: AppTheme = AppTheme.DARK,
    val materialYou: Boolean = false,
    val isAmoledBlack: Boolean = false,
    val colorStyle: PaletteStyle = PaletteStyle.Vibrant,
    val fontFamily: FontStyles = FontStyles.ALEXANDRIA,
    val primaryColor: ThemeColor = ThemeColor("Ocean Blue", 0xFF0099EE)
)

@Serializable
data class PersonalPreference(
    val maxStreakCount: Int = 0,
    val currentStreakCount: Int = 0,
    val lastLoginDate: Long = 0L,
    val isFirstLaunch: Boolean = true
)

@Serializable
data class SplashPreferences(
    val splashBackgroundColor: Long = 0x00000000,
    val splashTextColor: Long = 0xFFFFFFFF,
    val splashAnimationDuration: Int = 1000
)

@Serializable
data class OrbPreferences(
    val enabled: Boolean = true,
    val enableFloatingAnimation: Boolean = true,
    val enableScaleAnimation: Boolean = false,
    val enableAlphaAnimation: Boolean = false,
    val animationSpeed: Float = 1.0f
)