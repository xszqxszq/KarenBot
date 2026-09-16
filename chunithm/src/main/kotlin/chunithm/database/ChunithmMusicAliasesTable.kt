package xyz.xszq.bot.chunithm.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import xyz.xszq.bot.chunithm.music.MusicInfo
import xyz.xszq.bot.database.suspendedTransactionAsync

/**
 * 歌曲别名表
 */
@Suppress("unused")
object ChunithmMusicAliasesTable: Table() {
    val id = integer("id")
    val name = varchar("name", 128)
    val votes = integer("votes")

    override val primaryKey = PrimaryKey(id, name)

    /**
     * 获取歌曲所有的别名
     *
     * 仅限已投票通过
     *
     * @param music 歌曲信息
     * @return 别名列表
     */
    suspend operator fun get(
        music: MusicInfo
    ) = suspendedTransactionAsync {
        select(name, votes).where {
            (ChunithmMusicAliasesTable.id eq music.id) and (votes greaterEq 0)
        }.map { Pair(it[name], it[votes]) }
    }.await()

    /**
     * 获取歌曲别名的票数
     *
     * @param music 歌曲
     * @param alias 别名
     * @return 别名票数
     */
    suspend operator fun get(
        music: MusicInfo,
        alias: String
    ) = suspendedTransactionAsync {
        select(votes).where {
            (ChunithmMusicAliasesTable.id eq music.id) and (name eq alias)
        }.map { it[votes] }.firstOrNull()
    }.await()

    /**
     * 获取所有歌曲的投票通过的别名
     *
     * @return 歌曲 ID / 别名的列表
     */
    suspend fun all() = suspendedTransactionAsync {
        select(ChunithmMusicAliasesTable.id, name).where {
            votes greaterEq 0
        }.map { Pair(it[ChunithmMusicAliasesTable.id], it[name]) }
    }.await()

    /**
     * 根据别名查找歌曲
     *
     * @param alias 别名
     * @return 匹配到的歌曲 ID
     */
    suspend fun exact(
        alias: String
    ) = suspendedTransactionAsync {
        val cleaned = alias.trim().lowercase()
        select(ChunithmMusicAliasesTable.id).where {
            (name.lowerCase() eq cleaned) and (votes greaterEq 0)
        }.map { it[ChunithmMusicAliasesTable.id] }
    }.await()

    /**
     * 为歌曲别名投票
     *
     * @param music 歌曲
     * @param alias 别名
     */
    suspend fun vote(
        music: MusicInfo,
        alias: String
    ) = suspendedTransactionAsync {
        if (selectAll().where {
                (ChunithmMusicAliasesTable.id eq music.id) and (name eq alias)
            }.count() != 0L) {
            update({ (ChunithmMusicAliasesTable.id eq music.id) and (name eq alias) }) {
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

    /**
     * 不经过投票直接添加歌曲别名
     *
     * @param music 歌曲
     * @param alias 别名
     */
    suspend fun add(
        music: MusicInfo,
        alias: String
    ) = suspendedTransactionAsync {
        if (selectAll().where {
                (ChunithmMusicAliasesTable.id eq music.id) and (name eq alias)
            }.count() != 0L) {
            update({ (ChunithmMusicAliasesTable.id eq music.id) and (name eq alias) }) {
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

    /**
     * 删除歌曲别名
     *
     * @param music 歌曲
     * @param alias 别名
     */
    suspend fun remove(music: MusicInfo, alias: String) = suspendedTransactionAsync {
        ChunithmMusicAliasesTable.deleteWhere {
            (ChunithmMusicAliasesTable.id eq music.id) and (ChunithmMusicAliasesTable.name eq alias)
        }
    }.await()

    /**
     * 批量添加歌曲别名
     *
     * @param aliases 歌曲 ID 与别名的集合
     */
    suspend fun addAll(
        aliases: Collection<Pair<Int, String>>
    ) = suspendedTransactionAsync {
        val entries = aliases.distinct()
        if (entries.isEmpty()) return@suspendedTransactionAsync

        val musicIds = entries.map { it.first }.distinct()
        val existing = select(ChunithmMusicAliasesTable.id, name).where {
            ChunithmMusicAliasesTable.id inList musicIds
        }.map {
            it[ChunithmMusicAliasesTable.id] to it[name]
        }.toSet()

        val toInsert = entries.filterNot(existing::contains)
        val toReset = entries.filter(existing::contains)
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })

        toReset.forEach { (musicId, names) ->
            update({
                (ChunithmMusicAliasesTable.id eq musicId) and (name inList names)
            }) {
                it[votes] = 0
            }
        }

        batchInsert(toInsert, shouldReturnGeneratedValues = false) { entry: Pair<Int, String> ->
            this[ChunithmMusicAliasesTable.id] = entry.first
            this[name] = entry.second
            this[votes] = 0
        }
    }.await()
}