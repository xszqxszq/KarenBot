package xyz.xszq.bot.maimai.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import xyz.xszq.bot.maimai.music.MusicInfo

/**
 * 歌曲别名表
 */
@Suppress("unused")
object MaimaiMusicAliasesTable: Table() {
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
    suspend operator fun get(music: MusicInfo) = suspendedTransactionAsync {
        select(name, votes).where {
            (MaimaiMusicAliasesTable.id eq music.id) and (votes greaterEq 0)
        }.map { Pair(it[name], it[votes]) }
    }.await()

    /**
     * 获取歌曲别名的票数
     *
     * @param music 歌曲
     * @param alias 别名
     * @return 别名票数
     */
    suspend operator fun get(music: MusicInfo, alias: String) = suspendedTransactionAsync {
        select(votes).where {
            (MaimaiMusicAliasesTable.id eq music.id) and (name eq alias)
        }.map { it[votes] }.firstOrNull()
    }.await()

    /**
     * 获取所有歌曲的投票通过的别名
     *
     * @return 歌曲 ID / 别名的列表
     */
    suspend fun all() = suspendedTransactionAsync {
        select(MaimaiMusicAliasesTable.id, name).where {
            votes greaterEq 0
        }.map { Pair(it[MaimaiMusicAliasesTable.id], it[name]) }
    }.await()

    /**
     * 根据别名查找歌曲
     *
     * @param alias 别名
     * @return 匹配到的歌曲 ID
     */
    suspend fun exact(alias: String) = suspendedTransactionAsync {
        val cleaned = alias.trim().lowercase()
        select(MaimaiMusicAliasesTable.id).where {
            (name.lowerCase() eq cleaned) and (votes greaterEq 0)
        }.map { it[MaimaiMusicAliasesTable.id] }
    }.await()

    /**
     * 为歌曲别名投票
     *
     * @param music 歌曲
     * @param alias 别名
     */
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

    /**
     * 删除歌曲别名
     *
     * @param music 歌曲
     * @param alias 别名
     */
    suspend fun remove(music: MusicInfo, alias: String) = suspendedTransactionAsync {
        MaimaiMusicAliasesTable.deleteWhere {
            (MaimaiMusicAliasesTable.id eq music.id) and (MaimaiMusicAliasesTable.name eq alias)
        }
    }.await()

    /**
     * 不经过投票直接添加歌曲别名
     *
     * @param music 歌曲
     * @param alias 别名
     */
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