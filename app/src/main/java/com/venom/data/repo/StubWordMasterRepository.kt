package com.venom.data.repo

import com.venom.data.local.dao.WordMasterDao
import com.venom.data.mapper.WordMasterEntityMapper
import com.venom.domain.model.CefrLevel
import com.venom.domain.model.WordLevels
import com.venom.domain.model.WordMaster
import com.venom.domain.repo.IWordMasterRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubWordMasterRepository @Inject constructor(
    private val wordMasterDao: WordMasterDao
) : IWordMasterRepository {

    override suspend fun getWordById(id: Int): WordMaster? {
        return wordMasterDao.getWordById(id)?.let { WordMasterEntityMapper.toDomain(it) }
    }

    override suspend fun getWordsByIds(ids: List<Int>): List<WordMaster> {
        return wordMasterDao.getWordsByIds(ids).map { WordMasterEntityMapper.toDomain(it) }
    }

    override suspend fun getWordsByLevel(level: CefrLevel, limit: Int): List<WordMaster> {
        return wordMasterDao.getWordsByLevel(level.name, limit).map { WordMasterEntityMapper.toDomain(it) }
    }

    override suspend fun getWordsByCefr(cefrLevel: CefrLevel, limit: Int): List<WordMaster> {
        return wordMasterDao.getRandomWordsByCefr(cefrLevel.name, limit).map { WordMasterEntityMapper.toDomain(it) }
    }

    override suspend fun getLevelProgress(level: WordLevels): Float = 0f

    override suspend fun getLevelsProgress(): Map<String, Float> = emptyMap()

    override suspend fun getWordCount(): Int = wordMasterDao.getWordCount()

    override suspend fun insertWords(words: List<WordMaster>) {
        wordMasterDao.insertAll(words.map { WordMasterEntityMapper.toEntity(it) })
    }

    override suspend fun getRandomWords(limit: Int): List<WordMaster> {
        return wordMasterDao.getRandomWords(limit).map { WordMasterEntityMapper.toDomain(it) }
    }
}
