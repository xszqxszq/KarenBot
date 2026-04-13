package xyz.xszq.bot.chunithm.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import xyz.xszq.bot.chunithm.music.MusicInfo

object MusicAliasesTable: Table() {
    val id = integer("id")
    val name = varchar("name", 128)
    val votes = integer("votes")

    override val primaryKey = PrimaryKey(id, name)

    suspend operator fun get(
        music: MusicInfo
    ) = suspendedTransactionAsync {
        select(name, votes).where {
            (MusicAliasesTable.id eq music.id) and (votes greaterEq 0)
        }.map { Pair(it[name], it[votes]) }
    }.await()

    suspend operator fun get(
        music: MusicInfo,
        alias: String
    ) = suspendedTransactionAsync {
        select(votes).where {
            (MusicAliasesTable.id eq music.id) and (name eq alias)
        }.map { it[votes] }.firstOrNull()
    }.await()

    suspend fun all() = suspendedTransactionAsync {
        select(MusicAliasesTable.id, name).where {
            votes greaterEq 0
        }.map { Pair(it[MusicAliasesTable.id], it[name]) }
    }.await()

    suspend fun exact(
        alias: String
    ) = suspendedTransactionAsync {
        val cleaned = alias.trim().lowercase()
        select(MusicAliasesTable.id).where {
            (name.lowerCase() eq cleaned) and (votes greaterEq 0)
        }.map { it[MusicAliasesTable.id] }
    }.await()

    suspend fun vote(
        music: MusicInfo,
        alias: String
    ) = suspendedTransactionAsync {
        if (selectAll().where {
                (MusicAliasesTable.id eq music.id) and (name eq alias)
            }.count() != 0L) {
            update({ (MusicAliasesTable.id eq music.id) and (name eq alias) }) {
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

    suspend fun add(
        music: MusicInfo,
        alias: String
    ) = suspendedTransactionAsync {
        if (selectAll().where {
                (MusicAliasesTable.id eq music.id) and (name eq alias)
            }.count() != 0L) {
            update({ (MusicAliasesTable.id eq music.id) and (name eq alias) }) {
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

    suspend fun addAll(
        aliases: Collection<Pair<Int, String>>
    ) = suspendedTransactionAsync {
        val entries = aliases.distinct()
        if (entries.isEmpty()) return@suspendedTransactionAsync

        val musicIds = entries.map { it.first }.distinct()
        val existing = select(MusicAliasesTable.id, MusicAliasesTable.name).where {
            MusicAliasesTable.id inList musicIds
        }.map {
            it[MusicAliasesTable.id] to it[MusicAliasesTable.name]
        }.toSet()

        val toInsert = entries.filterNot(existing::contains)
        val toReset = entries.filter(existing::contains)
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })

        toReset.forEach { (musicId, names) ->
            update({
                (MusicAliasesTable.id eq musicId) and (MusicAliasesTable.name inList names)
            }) {
                it[votes] = 0
            }
        }

        batchInsert(toInsert, shouldReturnGeneratedValues = false) { entry: Pair<Int, String> ->
            this[MusicAliasesTable.id] = entry.first
            this[MusicAliasesTable.name] = entry.second
            this[votes] = 0
        }
    }.await()
}
