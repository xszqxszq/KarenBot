package xyz.xszq.bot.dao

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object DatabaseLock {
    val lock = Semaphore(32)
}
suspend fun <T> transactionWithLock(block: suspend Transaction.() -> T): T = DatabaseLock.lock.withPermit {
    runCatching {
        newSuspendedTransaction(Dispatchers.IO, statement = block)
    }.onFailure {
        it.printStackTrace()
    }
}.getOrThrow()