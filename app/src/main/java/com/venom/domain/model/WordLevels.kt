package com.venom.domain.model

import androidx.compose.ui.graphics.Color
import com.venom.wordvibe.R

sealed class WordLevels(
    val cefrLevel: CefrLevel,
    val titleRes: Int,
    val descRes: Int,
    val id: String,
    val iconRes: Int,
    val color: Color
) {
    object Beginner : WordLevels(
        cefrLevel = CefrLevel.A1,
        titleRes = R.string.level_beginner,
        descRes = R.string.desc_beginner,
        id = "A1",
        iconRes = R.drawable.ic_beginner_seedling,
        color = Color(0xFF155CFA)
    )

    object Elementary : WordLevels(
        cefrLevel = CefrLevel.A2,
        titleRes = R.string.level_elementary,
        descRes = R.string.desc_elementary,
        id = "A2",
        iconRes = R.drawable.ic_elementary_school,
        color = Color(0xFF10B981)
    )

    object Intermediate : WordLevels(
        cefrLevel = CefrLevel.B1,
        titleRes = R.string.level_intermediate,
        descRes = R.string.desc_intermediate,
        id = "B1",
        iconRes = R.drawable.ic_intermediate_book,
        color = Color(0xFFA855F7)
    )

    object UpperIntermediate : WordLevels(
        cefrLevel = CefrLevel.B2,
        titleRes = R.string.level_upper_intermediate,
        descRes = R.string.desc_upper_intermediate,
        id = "B2",
        iconRes = R.drawable.ic_upper_intermediate_brain,
        color = Color(0xFFF59E0B)
    )

    object Advanced : WordLevels(
        cefrLevel = CefrLevel.C1,
        titleRes = R.string.level_advanced,
        descRes = R.string.desc_advanced,
        id = "C1",
        iconRes = R.drawable.ic_advanced_column,
        color = Color(0xFF059669)
    )

    object Mastery : WordLevels(
        cefrLevel = CefrLevel.C2,
        titleRes = R.string.level_mastery,
        descRes = R.string.desc_mastery,
        id = "C2",
        iconRes = R.drawable.ic_mastery_trophy,
        color = Color(0xFF6366F1)
    )

    companion object {
        fun values(): List<WordLevels> = listOf(
            Beginner, Elementary, Intermediate, UpperIntermediate, Advanced, Mastery
        )

        fun fromCefr(level: CefrLevel): WordLevels = when (level) {
            CefrLevel.A1 -> Beginner
            CefrLevel.A2 -> Elementary
            CefrLevel.B1 -> Intermediate
            CefrLevel.B2 -> UpperIntermediate
            CefrLevel.C1 -> Advanced
            CefrLevel.C2 -> Mastery
        }
    }
}
