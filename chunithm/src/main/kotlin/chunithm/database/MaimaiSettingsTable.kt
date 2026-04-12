package xyz.xszq.bot.chunithm.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object MaimaiSettingsTable: Table() {
    val id = varchar("id", 32)
    val key = varchar("key", 32)
    val value = varchar("value", 512)

    override val primaryKey = PrimaryKey(id, key)

    suspend operator fun set(
        openId: String,
        key: String,
        value: String
    ) = newSuspendedTransaction {
        if (selectAll().where {
                (MaimaiSettingsTable.id eq openId) and (MaimaiSettingsTable.key eq key)
            }.count() != 0L)
            update({ (MaimaiSettingsTable.id eq openId) and (MaimaiSettingsTable.key eq key) }) {
                it[MaimaiSettingsTable.value] = value
            }
        else
            insert {
                it[MaimaiSettingsTable.id] = openId
                it[MaimaiSettingsTable.key] = key
                it[MaimaiSettingsTable.value] = value
            }
    }

    suspend operator fun get(
        openId: String,
        key: String
    ) = suspendedTransactionAsync {
        select(value).where {
            (MaimaiSettingsTable.id eq openId) and (MaimaiSettingsTable.key eq key)
        }.map { it[value] }.firstOrNull()?.let { it.ifBlank { null } }
    }.await()

    suspend fun defaultGame(
        openId: String
    ): String {
        return MaimaiSettingsTable[openId, "game-prior"] ?: "maimai"
    }

    suspend fun setDefaultGame(
        openId: String,
        game: String
    ) {
        MaimaiSettingsTable[openId, "game-prior"] = game
    }
}