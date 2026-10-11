package com.venom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "wordsMaster",
    indices = [
        androidx.room.Index(value = ["cefrLevel"]),
        androidx.room.Index(value = ["rank"])
    ]
)
data class WordMasterEntity(
    @PrimaryKey val id: Int,
    val wordEn: String? = null,
    val pos: String? = null,
    val cefrLevel: String? = null,
    val fromOxford: Int? = 0,
    val rank: Int? = null,
    val frequency: Int? = null,
    val difficultyScore: Int? = null,
    
    // Phonetics
    val phoneticUs: String? = null,
    val phoneticUk: String? = null,
    val phoneticAr: String? = null,
    val translit: String? = null,
    val syllabify: String? = null,
    
    // Semantics
    val definitionEn: String? = null,
    val definitionAr: String? = null,
    val usageNote: String? = null,
    val category: String? = null,
    val primarySense: String? = null,
    val semanticTags: String? = null,
    val register: String? = "Neutral",
    val mnemonicAr: String? = null,
    
    // Relational (JSON)
    val examples: String? = null,
    val collocations: String? = null,
    val synonyms: String? = null,
    val antonyms: String? = null,
    val relatedWords: String? = null,
    val wordFamily: String? = null,
    
    // Localization
    val arabicAr: String? = null,
    val frenchFr: String? = null,
    val germanDe: String? = null,
    val spanishEs: String? = null,
    val chineseZh: String? = null,
    val russianRu: String? = null,
    val portuguesePt: String? = null,
    val japaneseJa: String? = null,
    val italianIt: String? = null,
    val turkishTr: String? = null,

    // Unit grouping
    val unitId: String? = null,

    // Learning contract fields (frozen)
    val recallTarget: String? = null,
    val productionExpected: String? = null,
    val productionVariants: String? = null, // JSON array string
    val sourceRef: String? = null,
    val ieltsTopic: String? = null,
    val ieltsRelevance: String? = null,
    val reviewer: String? = null,
    val reviewedAt: String? = null,
    val checklist: String? = null // JSON array string
)
