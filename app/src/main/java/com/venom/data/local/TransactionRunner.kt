package com.venom.data.local

import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs a block inside one database transaction. Abstracted (instead of calling
 * `WordVibeDatabase.withTransaction` directly) so repository contracts stay unit-testable
 * on plain JVM without Room.
 */
interface TransactionRunner {
    suspend fun <T> withTransaction(block: suspend () -> T): T
}

@Singleton
class RoomTransactionRunner @Inject constructor(
    private val db: WordVibeDatabase
) : TransactionRunner {
    override suspend fun <T> withTransaction(block: suspend () -> T): T =
        db.withTransaction(block)
}
