@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xszq.bot.dao

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync

object Permissions: Table("permission") {
    val openId = varchar("openid", 32)
    val perm = varchar("perm", 64)
    val status = integer("status")
    const val DENIED = -1
    const val DELETED = 0
    const val ALLOWED = 1
    suspend fun getContextStatus(
        context: String,
        permName: String
    ) = suspendedTransactionAsync(Dispatchers.IO) {
        select {
            (openId eq context) and (perm eq permName) and (status neq DELETED)
        }.firstOrNull()
    }.await()
    suspend fun getGlobalStatus(
        permName: String
    ) = suspendedTransactionAsync(Dispatchers.IO) {
        select {
            (openId eq "*") and (perm eq permName) and (status neq DELETED)
        }.firstOrNull()
    }.await()
    suspend fun isPermitted(
        context: String,
        permName: String
    ): Boolean {
        if (permName.contains(".")) {
            if (!isPermitted(context, context.substringBefore(".")))
                return false
        }
        val globalPermStatus = getGlobalStatus(permName)
        val contextPermStatus = getContextStatus(context, permName)
        if (globalPermStatus ?.get(status) == DENIED)
            return contextPermStatus ?.get(status) == ALLOWED
        return contextPermStatus == null || contextPermStatus[status] == ALLOWED
    }
    suspend fun isNotPermitted(
        context: String,
        permName: String
    ) = !isPermitted(context, permName)
    suspend fun setPerm(
        context: String,
        permName: String,
        allowed: Boolean
    ) = newSuspendedTransaction(Dispatchers.IO) {
        upsert {
            it[openId] = context
            it[perm] = permName
            it[status] = if (allowed) ALLOWED else DENIED
        }
    }
}