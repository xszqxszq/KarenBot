package xyz.xszq.otomadbot.kotlin

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime

val pass = {}


suspend fun <T> transactionWithLock(block: suspend Transaction.() -> T): T {
    return runCatching {
        newSuspendedTransaction(Dispatchers.IO, statement = block)
    }.onFailure {
        it.printStackTrace()
    }.getOrThrow()
}

fun LocalDateTime.isSameDay(b: LocalDateTime): Boolean =
    year == b.year && month == b.month && dayOfMonth == b.dayOfMonth