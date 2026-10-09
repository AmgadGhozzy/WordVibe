package com.venom.domain.repo

import com.venom.domain.model.CefrLevel
import com.venom.domain.model.WordLevels
import com.venom.domain.model.WordMaster

interface IWordMasterRepository {

    suspend fun getWordById(id: Int): WordMaster?

    suspend fun getWordsByIds(ids: List<Int>): List<WordMaster>

    suspend fun getWordsByLevel(level: CefrLevel, limit: Int = 50): List<WordMaster>

    /** Get random words for a specific CEFR level. */
    suspend fun getWordsByCefr(cefrLevel: CefrLevel, limit: Int = 20): List<WordMaster>

    suspend fun getLevelProgress(level: WordLevels): Float

    suspend fun getLevelsProgress(): Map<String, Float>

    suspend fun getWordCount(): Int

    /** Words available at a CEFR level (placement floor, ADR-0010). */
    suspend fun countWordsByCefr(level: CefrLevel): Int

    suspend fun insertWords(words: List<WordMaster>)

    /**
     * Ticket 15 (versioned seeding): INSERT OR IGNORE semantics — an existing
     * word (reviewed pilot row or previously-seeded expansion row) is never
     * replaced.
     */
    suspend fun insertWordsOrIgnore(words: List<WordMaster>)

    /** Get random words (for fallback / topic-based loading). */
    suspend fun getRandomWords(limit: Int = 15): List<WordMaster>
}
