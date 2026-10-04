package com.venom.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.venom.wordvibe.R
import kotlinx.serialization.Serializable

/**
 * Model representing a translation service provider
 *
 * @property id Unique identifier for the provider
 * @property nameResId String resource ID for the provider name
 * @property iconResId Drawable resource ID for the provider icon
 * @property descriptionResId String resource ID for provider description
 * @property isPremium Whether this provider requires a premium subscription
 */
@Serializable
data class TranslationProvider(
    val id: String,
    @StringRes val nameResId: Int,
    @DrawableRes val iconResId: Int,
    @StringRes val descriptionResId: Int,
    val isPremium: Boolean = false,
) {

    companion object {
        val GOOGLE = TranslationProvider(
            id = "google",
            nameResId = R.string.provider_google,
            iconResId = R.drawable.icon_google,
            descriptionResId = R.string.provider_google_description
        )

        val OFFLINE = TranslationProvider(
            id = "offline",
            nameResId = R.string.provider_offline,
            iconResId = R.drawable.icon_offline,
            descriptionResId = R.string.provider_offline_description
        )

        val CHATGPT = TranslationProvider(
            id = "chatgpt",
            nameResId = R.string.provider_chatgpt,
            iconResId = R.drawable.icon_chatgpt,
            descriptionResId = R.string.provider_chatgpt_description,
            isPremium = true
        )

        val GEMINI = TranslationProvider(
            id = "Gemini",
            nameResId = R.string.provider_gemini,
            iconResId = R.drawable.icon_gemini,
            descriptionResId = R.string.provider_gemini_description
        )

        val GROQ = TranslationProvider(
            id = "Groq",
            nameResId = R.string.provider_groq,
            iconResId = R.drawable.icon_groq,
            descriptionResId = R.string.provider_groq_description
        )

        val DEEPSEEK = TranslationProvider(
            id = "DeepSeek",
            nameResId = R.string.provider_deepseek,
            iconResId = R.drawable.icon_deepseek,
            descriptionResId = R.string.provider_deepseek_description
        )

        val HUGGINGFACE = TranslationProvider(
            id = "HuggingFace",
            nameResId = R.string.provider_huggingface,
            iconResId = R.drawable.icon_huggingface,
            descriptionResId = R.string.provider_huggingface_description
        )

        val ALL = listOf(GOOGLE,OFFLINE, CHATGPT, GEMINI, GROQ, DEEPSEEK, HUGGINGFACE)
    }
}