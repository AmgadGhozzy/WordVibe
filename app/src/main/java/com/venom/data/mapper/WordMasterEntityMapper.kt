package com.venom.data.mapper

import org.json.JSONArray
import org.json.JSONObject
import com.venom.data.local.entity.WordMasterEntity
import com.venom.data.remote.respnod.RelatedWordsDto
import com.venom.data.remote.respnod.WordFamilyDto
import com.venom.data.remote.respnod.WordMasterDto
import com.venom.domain.model.CefrLevel
import com.venom.domain.model.RelatedWords
import com.venom.domain.model.WordFamily
import com.venom.domain.model.WordMaster

/**
 * Mapper between WordMasterEntity and WordMaster domain model.
 */
object WordMasterEntityMapper {
    
    fun toDomain(entity: WordMasterEntity): WordMaster {
        val cefrLevel = try {
            CefrLevel.valueOf(entity.cefrLevel?.uppercase() ?: "B1")
        } catch (_: Exception) {
            CefrLevel.B1
        }
        
        val semanticTagsList: List<String> = entity.semanticTags?.let {
            parseJsonList(it)
        } ?: emptyList()
        
        val synonymsList: List<String> = entity.synonyms?.let { parseJsonList(it) } ?: emptyList()
        val antonymsList: List<String> = entity.antonyms?.let { parseJsonList(it) } ?: emptyList()
        val collocationsList: List<String> = entity.collocations?.let { parseJsonList(it) } ?: emptyList()
        
        val examplesMap: Map<CefrLevel, String> = entity.examples?.let {
            parseExamples(it)
        } ?: emptyMap()
        
        val wordFamilyObj = entity.wordFamily?.let { parseWordFamily(it) } ?: WordFamily()
        val relatedWordsObj = entity.relatedWords?.let { parseRelatedWords(it) } ?: RelatedWords(emptyList(), emptyList())
        
        return WordMaster(
            id = entity.id,
            wordEn = entity.wordEn ?: "",
            pos = entity.pos ?: "noun",
            cefrLevel = cefrLevel,
            fromOxford = entity.fromOxford ?: 0,
            rank = entity.rank ?: 0,
            frequency = entity.frequency ?: 1,
            difficultyScore = entity.difficultyScore ?: 5,
            phoneticUs = entity.phoneticUs ?: "",
            phoneticUk = entity.phoneticUk,
            phoneticAr = entity.phoneticAr ?: "",
            translit = entity.translit ?: "",
            syllabify = entity.syllabify,
            definitionEn = entity.definitionEn ?: "",
            definitionAr = entity.definitionAr,
            usageNote = entity.usageNote,
            category = entity.category,
            primarySense = entity.primarySense,
            semanticTags = semanticTagsList,
            register = entity.register ?: "Neutral",
            mnemonicAr = entity.mnemonicAr,
            wordFamily = wordFamilyObj,
            synonyms = synonymsList,
            antonyms = antonymsList,
            examples = examplesMap,
            collocations = collocationsList,
            relatedWords = relatedWordsObj,
            arabicAr = entity.arabicAr ?: "",
            frenchFr = entity.frenchFr,
            germanDe = entity.germanDe,
            spanishEs = entity.spanishEs,
            chineseZh = entity.chineseZh,
            russianRu = entity.russianRu,
            portuguesePt = entity.portuguesePt,
            japaneseJa = entity.japaneseJa,
            italianIt = entity.italianIt,
            turkishTr = entity.turkishTr,
            unitId = entity.unitId,
            recallTarget = entity.recallTarget,
            productionExpected = entity.productionExpected,
            productionVariants = entity.productionVariants?.let { parseJsonList(it) }
                ?: emptyList(),
            sourceRef = entity.sourceRef,
            ieltsTopic = entity.ieltsTopic,
            ieltsRelevance = entity.ieltsRelevance,
            reviewer = entity.reviewer,
            reviewedAt = entity.reviewedAt,
            checklist = entity.checklist?.let { parseJsonList(it) } ?: emptyList()
        )

    }

    /**
     * Map WordMasterDto to WordMaster domain model.
     */
    fun toDomain(dto: WordMasterDto, fallbackId: Int = 0): WordMaster {
        // Parse CEFR Level safely
        val cefrLevelEnum = try {
            CefrLevel.valueOf(dto.cefrLevel.uppercase())
        } catch (e: IllegalArgumentException) {
            CefrLevel.B1 // Default fallback
        }

        // Parse frequency as Int
        val frequencyInt = dto.frequency?.toIntOrNull() ?: (cefrLevelEnum.ordinal + 1)

        // Parse difficulty score
        val difficultyInt = dto.difficultyScore?.toIntOrNull()
            ?: calculateDifficulty(dto.wordEn, dto.syllabify, cefrLevelEnum)

        // Parse fromOxford
        val fromOxfordInt = dto.fromOxford?.toIntOrNull() ?: 0

        // Parse examples map
        val examplesMap = dto.examples?.mapNotNull { (key, value) ->
            try {
                CefrLevel.valueOf(key.uppercase()) to value
            } catch (e: IllegalArgumentException) {
                null
            }
        }?.toMap() ?: emptyMap()

        return WordMaster(
            id = dto.id ?: fallbackId,
            wordEn = dto.wordEn,
            pos = dto.pos,
            cefrLevel = cefrLevelEnum,
            fromOxford = fromOxfordInt,
            rank = dto.rank ?: 0,
            frequency = frequencyInt,
            difficultyScore = difficultyInt,
            phoneticUs = dto.phoneticUs,
            phoneticUk = dto.phoneticUk,
            phoneticAr = dto.phoneticAr,
            translit = dto.translit,
            syllabify = dto.syllabify,
            definitionEn = dto.definitionEn,
            definitionAr = dto.definitionAr,
            usageNote = dto.usageNote,
            category = dto.category,
            primarySense = dto.primarySense,
            semanticTags = dto.semanticTags ?: emptyList(),
            register = dto.register ?: "Neutral",
            mnemonicAr = dto.mnemonicAr,
            wordFamily = dto.wordFamily?.toDomain() ?: WordFamily(),
            synonyms = dto.synonyms ?: emptyList(),
            antonyms = dto.antonyms ?: emptyList(),
            examples = examplesMap,
            collocations = dto.collocations ?: emptyList(),
            relatedWords = dto.relatedWords?.toDomain() ?: RelatedWords(emptyList(), emptyList()),
            arabicAr = dto.arabicAr,
            frenchFr = dto.frenchFr,
            germanDe = dto.germanDe,
            spanishEs = dto.spanishEs,
            chineseZh = dto.chineseZh,
            russianRu = dto.russianRu,
            portuguesePt = dto.portuguesePt,
            japaneseJa = dto.japaneseJa,
            italianIt = dto.italianIt,
            turkishTr = dto.turkishTr,
            recallTarget = dto.recallTarget,
            productionExpected = dto.productionExpected,
            productionVariants = dto.productionVariants ?: emptyList(),
            sourceRef = dto.sourceRef,
            ieltsTopic = dto.ieltsTopic,
            ieltsRelevance = dto.ieltsRelevance,
            reviewer = dto.reviewer,
            reviewedAt = dto.reviewedAt,
            checklist = dto.checklist ?: emptyList()
        )
    }

    private fun WordFamilyDto.toDomain(): WordFamily {
        return WordFamily(
            noun = noun?.takeIf { it.isNotBlank() },
            verb = verb?.takeIf { it.isNotBlank() },
            adj = adj?.takeIf { it.isNotBlank() },
            adv = adv?.takeIf { it.isNotBlank() }
        )
    }

    private fun RelatedWordsDto.toDomain(): RelatedWords {
        return RelatedWords(
            english = en ?: emptyList(),
            arabic = ar ?: emptyList()
        )
    }

    private fun calculateDifficulty(
        word: String,
        syllabify: String?,
        cefrLevel: CefrLevel
    ): Int {
        val syllableCount = syllabify?.count { it == '•' }?.plus(1) ?: 1
        val lengthFactor = word.length * 0.2
        val syllableFactor = syllableCount * 0.4
        val cefrFactor = cefrLevel.ordinal * 1.0

        return (lengthFactor + syllableFactor + cefrFactor)
            .toInt()
            .coerceIn(1, 10)
    }
    
    fun toEntity(domain: WordMaster): WordMasterEntity {
        return WordMasterEntity(
            id = domain.id,
            wordEn = domain.wordEn,
            pos = domain.pos,
            cefrLevel = domain.cefrLevel.name,
            fromOxford = domain.fromOxford,
            rank = domain.rank,
            frequency = domain.frequency,
            difficultyScore = domain.difficultyScore,
            phoneticUs = domain.phoneticUs,
            phoneticUk = domain.phoneticUk,
            phoneticAr = domain.phoneticAr,
            translit = domain.translit,
            syllabify = domain.syllabify,
            definitionEn = domain.definitionEn,
            definitionAr = domain.definitionAr,
            usageNote = domain.usageNote,
            category = domain.category,
            primarySense = domain.primarySense,
            semanticTags = JSONArray(domain.semanticTags).toString(),
            register = domain.register,
            mnemonicAr = domain.mnemonicAr,
            wordFamily = JSONObject().apply {
                put("noun", domain.wordFamily.noun ?: JSONObject.NULL)
                put("verb", domain.wordFamily.verb ?: JSONObject.NULL)
                put("adj", domain.wordFamily.adj ?: JSONObject.NULL)
                put("adv", domain.wordFamily.adv ?: JSONObject.NULL)
            }.toString(),
            synonyms = JSONArray(domain.synonyms).toString(),
            antonyms = JSONArray(domain.antonyms).toString(),
            examples = JSONObject(domain.examples.mapKeys { it.key.name }).toString(),
            collocations = JSONArray(domain.collocations).toString(),
            relatedWords = JSONObject().apply {
                put("en", JSONArray(domain.relatedWords.english))
                put("ar", JSONArray(domain.relatedWords.arabic))
            }.toString(),
            arabicAr = domain.arabicAr,
            frenchFr = domain.frenchFr,
            germanDe = domain.germanDe,
            spanishEs = domain.spanishEs,
            chineseZh = domain.chineseZh,
            russianRu = domain.russianRu,
            portuguesePt = domain.portuguesePt,
            japaneseJa = domain.japaneseJa,
            italianIt = domain.italianIt,
            turkishTr = domain.turkishTr,
            unitId = domain.unitId,
            recallTarget = domain.recallTarget,
            productionExpected = domain.productionExpected,
            productionVariants = JSONArray(domain.productionVariants).toString(),
            sourceRef = domain.sourceRef,
            ieltsTopic = domain.ieltsTopic,
            ieltsRelevance = domain.ieltsRelevance,
            reviewer = domain.reviewer,
            reviewedAt = domain.reviewedAt,
            checklist = JSONArray(domain.checklist).toString()
        )
    }
    
    fun toEntityList(domains: List<WordMaster>): List<WordMasterEntity> = domains.map { toEntity(it) }
    
    private fun parseJsonList(json: String): List<String> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { array.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }
    
    private fun parseExamples(json: String): Map<CefrLevel, String> {
        return try {
            val obj = JSONObject(json)
            val keys = obj.keys()
            val result = mutableMapOf<CefrLevel, String>()
            while (keys.hasNext()) {
                val key = keys.next()
                try {
                    result[CefrLevel.valueOf(key.uppercase())] = obj.getString(key)
                } catch (_: Exception) { }
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }
    
    private fun parseWordFamily(json: String): WordFamily {
        return try {
            val obj = JSONObject(json)
            fun extractField(key: String): String? {
                if (!obj.has(key) || obj.isNull(key)) return null
                return obj.getString(key).takeIf { it.isNotBlank() }
            }
            WordFamily(
                noun = extractField("noun"),
                verb = extractField("verb"),
                adj = extractField("adj"),
                adv = extractField("adv")
            )
        } catch (e: Exception) {
            WordFamily()
        }
    }
    
    private fun parseRelatedWords(json: String): RelatedWords {
        return try {
            val obj = JSONObject(json)
            val enArray = obj.optJSONArray("en") ?: obj.optJSONArray("english")
            val arArray = obj.optJSONArray("ar") ?: obj.optJSONArray("arabic")
            RelatedWords(
                english = if (enArray != null) (0 until enArray.length()).map { enArray.getString(it) } else emptyList(),
                arabic = if (arArray != null) (0 until arArray.length()).map { arArray.getString(it) } else emptyList()
            )
        } catch (e: Exception) {
            RelatedWords(emptyList(), emptyList())
        }
    }
}
