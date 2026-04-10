package xyz.xszq.bot.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.update

object DivingFishBindTable: Table() {
    val id = varchar("id", 32)
    val importToken = text("importToken")
    override val primaryKey = PrimaryKey(id)
    suspend fun update(openId: String, importToken: String) = newSuspendedTransaction {
        if (selectAll().where {
                DivingFishBindTable.id eq openId
            }.count() != 0L)
            update({ DivingFishBindTable.id eq openId }) {
                it[DivingFishBindTable.importToken] = importToken
            }
        else
            insert {
                it[id] = openId
                it[DivingFishBindTable.importToken] = importToken
            }
    }
    suspend operator fun get(openId: String) = suspendedTransactionAsync {
        select(importToken).where {
            DivingFishBindTable.id eq openId
        }.map { it[importToken] }.singleOrNull()
    }.await()
}