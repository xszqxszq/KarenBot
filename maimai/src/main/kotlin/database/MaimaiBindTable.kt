package xyz.xszq.bot.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.update

object MaimaiBindTable: Table() {
    val id = varchar("id", 32)
    val userId = long("userId")
    override val primaryKey = PrimaryKey(id)
    suspend fun update(openId: String, userId: Long) = suspendedTransactionAsync {
        if (selectAll().where {
                MaimaiBindTable.id eq openId
            }.count() != 0L)
            update({ MaimaiBindTable.id eq openId }) {
                it[MaimaiBindTable.userId] = userId
            }
        else
            insert {
                it[id] = openId
                it[MaimaiBindTable.userId] = userId
            }
    }
    suspend operator fun get(openId: String) = suspendedTransactionAsync {
        select(userId).where {
            MaimaiBindTable.id eq openId
        }.map { it[userId] }.singleOrNull()
    }.await()
}