package com.venom.domain.model

import com.venom.wordvibe.R

enum class UserLevel(
    val displayName: String,
    val minXp: Int,
    val iconRes: Int
) {
    NEWCOMER    ("Newcomer",     0,     R.drawable.ic_beginner_seedling),
    EXPLORER    ("Explorer",     450,   R.drawable.ic_beginner_seedling),
    LEARNER     ("Learner",      900,   R.drawable.ic_elementary_school),
    APPRENTICE  ("Apprentice",   1375,  R.drawable.ic_elementary_school),
    SCHOLAR     ("Scholar",      1850,  R.drawable.ic_intermediate_book),
    ADEPT       ("Adept",        2350,  R.drawable.ic_intermediate_book),
    EXPERT      ("Expert",       2850,  R.drawable.ic_upper_intermediate_brain),
    MASTER      ("Master",       3800,  R.drawable.ic_upper_intermediate_brain),
    CHAMPION    ("Champion",     4750,  R.drawable.ic_advanced_column),
    LEGEND      ("Legend",       5675,  R.drawable.ic_advanced_column),
    POLYGLOT    ("Polyglot",     6600,  R.drawable.ic_mastery_trophy),
    SAGE        ("Sage",         6700,  R.drawable.ic_mastery_trophy);

    companion object {
        fun fromXp(totalXp: Int): UserLevel =
            entries.lastOrNull { totalXp >= it.minXp } ?: NEWCOMER

        fun xpToNextLevel(totalXp: Int): Int {
            val currentLevel = fromXp(totalXp)
            val nextLevel = entries.getOrNull(currentLevel.ordinal + 1)
                ?: return 0
            return nextLevel.minXp - totalXp
        }

        fun levelProgress(totalXp: Int): Float {
            val current = fromXp(totalXp)
            val next = entries.getOrNull(current.ordinal + 1) ?: return 1f
            val levelXpRange = next.minXp - current.minXp
            val xpIntoLevel = totalXp - current.minXp
            return (xpIntoLevel.toFloat() / levelXpRange).coerceIn(0f, 1f)
        }

        fun levelNumber(totalXp: Int): Int = fromXp(totalXp).ordinal + 1

        fun baseUserLevelForCefr(cefrLevel: CefrLevel): UserLevel = when (cefrLevel) {
            CefrLevel.A1 -> NEWCOMER
            CefrLevel.A2 -> LEARNER
            CefrLevel.B1 -> SCHOLAR
            CefrLevel.B2 -> EXPERT
            CefrLevel.C1 -> CHAMPION
            CefrLevel.C2 -> POLYGLOT
        }
    }
}