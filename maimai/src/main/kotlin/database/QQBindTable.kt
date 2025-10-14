package xyz.xszq.bot.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.update

object QQBindTable: Table() {
    val id = varchar("id", 32)
    val qq = long("qq")
    override val primaryKey = PrimaryKey(id)
    suspend fun update(openId: String, qq: Long) = suspendedTransactionAsync {
        if (selectAll().where {
                QQBindTable.id eq openId
            }.count() != 0L)
            update({ QQBindTable.id eq openId }) {
                it[QQBindTable.qq] = qq
            }
        else
            insert {
                it[id] = openId
                it[QQBindTable.qq] = qq
            }
    }
    suspend operator fun get(openId: String) = suspendedTransactionAsync {
        select(qq).where {
            QQBindTable.id eq openId
        }.map { it[qq] }.firstOrNull()
    }.await()

    suspend fun hasBinding(openId: String) = suspendedTransactionAsync {
        selectAll().where {
            QQBindTable.id eq openId
        }.count() > 0
    }.await()
}