@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xszq.bot.dao

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync

object Permissions: Table("permission") {
    val openId = varchar("openid", 32)
    val perm = varchar("perm", 64)
    val status = integer("status")
    const val DENIED = -1
    const val DELETED = 0
    const val ALLOWED = 1
    var permissions = mapOf<Pair<String, String>, Int>()
    val mutex = Mutex()
    var cacheTime = 0L
    private suspend fun fetchFromDB() = transactionWithLock {
        selectAll().associateBy ({ Pair(it[openId], it[perm]) }, { it[status] })
    }
    private suspend fun update(force: Boolean = false) {
        mutex.withLock {
            if (force || System.currentTimeMillis() - cacheTime >= 60 * 1000L) {
                permissions = fetchFromDB()
            }
            cacheTime = System.currentTimeMillis()
        }
    }
    suspend fun getContextStatus(
        context: String,
        permName: String
    ): Int? {
        update()
        val now = permissions[Pair(context, permName)]
        if (now != DELETED)
            return now
        return null
    }
    suspend fun getGlobalStatus(
        permName: String
    ): Int? {
        update()
        val now = permissions[Pair("*", permName)]
        if (now != DELETED)
            return now
        return null
    }
    suspend fun isPermitted(
        context: String,
        permName: String
    ): Boolean {
        if (permName.contains(".")) {
            if (!isPermitted(context, permName.substringBefore(".")))
                return false
        }
        val globalPermStatus = getGlobalStatus(permName)
        val contextPermStatus = getContextStatus(context, permName)
        if (globalPermStatus == DENIED)
            return contextPermStatus == ALLOWED
        return contextPermStatus == null || contextPermStatus == ALLOWED
    }
    suspend fun isNotPermitted(
        context: String,
        permName: String
    ) = !isPermitted(context, permName)
    suspend fun setPerm(
        context: String,
        permName: String,
        allowed: Boolean
    ) = withContext(Dispatchers.IO) {
        transactionWithLock {
            upsert {
                it[openId] = context
                it[perm] = permName
                it[status] = if (allowed) ALLOWED else DENIED
            }
        }
        update(true)
    }
}