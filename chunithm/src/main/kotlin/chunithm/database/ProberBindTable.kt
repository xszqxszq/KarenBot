package xyz.xszq.bot.chunithm.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync

object ProberBindTable: Table() {
    val id = varchar("id", 32)
    val prober = varchar("prober", 32)
    val key = varchar("key", 32)
    val value = text("value")
    override val primaryKey = PrimaryKey(id, prober, key)
    suspend operator fun set(
        id: String,
        prober: String,
        key: String,
        value: String
    ) = newSuspendedTransaction {
        if (selectAll().where {
                (ProberBindTable.id eq id) and (ProberBindTable.prober eq prober) and
                        (ProberBindTable.key eq key)
            }.count() != 0L)
            update({ (ProberBindTable.id eq id) and (ProberBindTable.prober eq prober) and
                    (ProberBindTable.key eq key) }) {
                it[ProberBindTable.value] = value
            }
        else
            insert {
                it[ProberBindTable.id] = id
                it[ProberBindTable.prober] = prober
                it[ProberBindTable.key] = key
                it[ProberBindTable.value] = value
            }
    }
    suspend operator fun get(
        id: String,
        prober: String,
        key: String
    ) = suspendedTransactionAsync {
        select(value).where {
            (ProberBindTable.id eq id) and (ProberBindTable.prober eq prober) and
                    (ProberBindTable.key eq key)
        }.map { it[value] }.firstOrNull() ?.let { it.ifBlank { null } }
    }.await()
    suspend fun findIdByValue(
        prober: String,
        key: String,
        bindValue: String
    ) = suspendedTransactionAsync {
        select(ProberBindTable.id).where {
            (ProberBindTable.prober eq prober) and (ProberBindTable.key eq key) and
                    (ProberBindTable.value eq bindValue)
        }.map { it[ProberBindTable.id] }.firstOrNull()
    }.await()
}
