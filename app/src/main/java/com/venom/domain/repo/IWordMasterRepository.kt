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

    suspend fun insertWords(words: List<WordMaster>)

    /** Get random words (for fallback / topic-based loading). */
    suspend fun getRandomWords(limit: Int = 15): List<WordMaster>
}
