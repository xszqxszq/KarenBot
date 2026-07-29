package xyz.xszq.bot.chunithm.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync

object QQBindTable: Table() {
    val id = varchar("id", 32)
    val qq = long("qq")
    override val primaryKey = PrimaryKey(id)
    suspend operator fun get(openId: String) = suspendedTransactionAsync {
        select(qq).where {
            QQBindTable.id eq openId
        }.map { it[qq] }.firstOrNull()
    }.await()
}