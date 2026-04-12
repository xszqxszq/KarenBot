package xyz.xszq.bot.maimai.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import xyz.xszq.bot.maimai.music.MusicInfo

object MusicAliasesVoteTable: Table() {
    val id = integer("id")
    val name = varchar("name", 128)
    val user = varchar("user", 32)

    override val primaryKey = PrimaryKey(id, name, user)
    suspend fun vote(
        music: MusicInfo,
        alias: String,
        openId: String
    ) = suspendedTransactionAsync {
        if (selectAll().where {
                (MusicAliasesVoteTable.id eq music.id) and (name eq alias) and (user eq openId)
            }.count() > 0)
            return@suspendedTransactionAsync
        else
            insert {
                it[id] = music.id
                it[name] = alias
                it[user] = openId
            }
    }.await()
    suspend operator fun get(
        music: MusicInfo,
        alias: String,
        openId: String
    ) = suspendedTransactionAsync {
        selectAll().where {
            (MusicAliasesVoteTable.id eq music.id) and (name eq alias) and (user eq openId)
        }.count() > 0
    }.await()
}