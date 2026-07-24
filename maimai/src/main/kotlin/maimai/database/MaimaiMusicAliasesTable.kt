package xyz.xszq.bot.maimai.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import xyz.xszq.bot.maimai.music.MusicInfo

object MaimaiMusicAliasesTable: Table() {
    val id = integer("id")
    val name = varchar("name", 128)
    val votes = integer("votes")
    override val primaryKey = PrimaryKey(id, name)
    suspend operator fun get(music: MusicInfo) = suspendedTransactionAsync {
        select(name, votes).where {
            (MaimaiMusicAliasesTable.id eq music.id) and (votes greaterEq 0)
        }.map { Pair(it[name], it[votes]) }
    }.await()
    suspend operator fun get(music: MusicInfo, alias: String) = suspendedTransactionAsync {
        select(votes).where {
            (MaimaiMusicAliasesTable.id eq music.id) and (name eq alias)
        }.map { it[votes] }.firstOrNull()
    }.await()
    suspend fun all() = suspendedTransactionAsync {
        select(MaimaiMusicAliasesTable.id, name).where {
            votes greaterEq 0
        }.map { Pair(it[MaimaiMusicAliasesTable.id], it[name]) }
    }.await()
    suspend fun exact(alias: String) = suspendedTransactionAsync {
        val cleaned = alias.trim().lowercase()
        select(MaimaiMusicAliasesTable.id).where {
            (name.lowerCase() eq cleaned) and (votes greaterEq 0)
        }.map { it[MaimaiMusicAliasesTable.id] }
    }.await()
    suspend fun vote(music: MusicInfo, alias: String) = suspendedTransactionAsync {
        if (selectAll().where {
                (MaimaiMusicAliasesTable.id eq music.id) and (name eq alias)
            }.count() != 0L) {
            update({ (MaimaiMusicAliasesTable.id eq music.id) and (name eq alias) }) {
                with(SqlExpressionBuilder) {
                    it[votes] = votes + 1
                }
            }
        } else {
            insert {
                it[id] = music.id
                it[name] = alias
                it[votes] = -2
            }
        }
    }.await()
    suspend fun remove(music: MusicInfo, alias: String) = suspendedTransactionAsync {
        MaimaiMusicAliasesTable.deleteWhere {
            (MaimaiMusicAliasesTable.id eq music.id) and (MaimaiMusicAliasesTable.name eq alias)
        }
    }.await()
    suspend fun add(music: MusicInfo, alias: String) = suspendedTransactionAsync {
        if (selectAll().where {
                (MaimaiMusicAliasesTable.id eq music.id) and (name eq alias)
            }.count() != 0L) {
            update({ (MaimaiMusicAliasesTable.id eq music.id) and (name eq alias) }) {
                it[votes] = 0
            }
        } else {
            insert {
                it[id] = music.id
                it[name] = alias
                it[votes] = 0
            }
        }
    }.await()
}