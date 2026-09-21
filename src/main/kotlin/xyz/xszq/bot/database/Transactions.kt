package xyz.xszq.bot.database

import kotlinx.coroutines.Deferred
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import xyz.xszq.bot.util.Metrics
import xyz.xszq.bot.util.dbDispatcher
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction as exposedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync as exposedTransactionAsync

/**
 * 在数据库调度器上执行事务
 *
 * @param db 数据库连接
 * @param transactionIsolation 事务隔离级别
 * @param readOnly 是否只读
 * @param statement 事务代码块
 * @return 事务结果
 */
suspend fun <T> newSuspendedTransaction(
    db: Database ?= null,
    transactionIsolation: Int ?= null,
    readOnly: Boolean ?= null,
    statement: suspend Transaction.() -> T
): T = exposedTransaction(
    context = dbDispatcher,
    db = db,
    transactionIsolation = transactionIsolation,
    readOnly = readOnly,
    statement = {
        Metrics.time(
            "karenbot.db.transaction",
            "mode" to "sync",
            "access" to when (readOnly) {
                true -> "read"
                false -> "write"
                null -> "default"
            }
        ) {
            statement()
        }
    }
)

/**
 * 在数据库调度器上异步执行事务
 *
 * @param db 数据库连接
 * @param transactionIsolation 事务隔离级别
 * @param readOnly 是否只读
 * @param statement 事务代码块
 * @return 事务结果
 */
suspend fun <T> suspendedTransactionAsync(
    db: Database ?= null,
    transactionIsolation: Int ?= null,
    readOnly: Boolean ?= null,
    statement: suspend Transaction.() -> T
): Deferred<T> = exposedTransactionAsync(
    context = dbDispatcher,
    db = db,
    transactionIsolation = transactionIsolation,
    readOnly = readOnly,
    statement = {
        Metrics.time(
            "karenbot.db.transaction",
            "mode" to "async",
            "access" to when (readOnly) {
                true -> "read"
                false -> "write"
                null -> "default"
            }
        ) {
            statement()
        }
    }
)