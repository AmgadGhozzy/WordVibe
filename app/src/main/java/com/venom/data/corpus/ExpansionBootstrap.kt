package com.venom.data.corpus

object ExpansionBootstrap {
    const val EXPANSION_BATCH_VERSION = 2

    suspend fun seedAndExpand(seeder: CorpusSeeder, pilotJson: String, expansionJson: String): Int {
        seeder.seedIfNeeded(pilotJson)
        return seeder.applyExpansionBatch(expansionJson, EXPANSION_BATCH_VERSION)
    }
}
