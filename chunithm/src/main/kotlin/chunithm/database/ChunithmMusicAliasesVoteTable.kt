package xyz.xszq.bot.chunithm.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import xyz.xszq.bot.chunithm.music.MusicInfo

/**
 * 歌曲别名投票记录表
 */
@Suppress("unused")
object ChunithmMusicAliasesVoteTable: Table() {
    val id = integer("id")
    val name = varchar("name", 128)
    val user = varchar("user", 32)

    override val primaryKey = PrimaryKey(id, name, user)

    /**
     * 记录一次投票
     *
     * @param music 歌曲
     * @param alias 别名
     * @param openId 投票用户的 OpenID
     */
    suspend fun vote(
        music: MusicInfo,
        alias: String,
        openId: String
    ) = suspendedTransactionAsync {
        if (selectAll().where {
                (ChunithmMusicAliasesVoteTable.id eq music.id) and (name eq alias) and (user eq openId)
            }.count() > 0)
            return@suspendedTransactionAsync
        else
            insert {
                it[id] = music.id
                it[name] = alias
                it[user] = openId
            }
    }.await()

    /**
     * 判断用户是否已为该别名投过票
     *
     * @param music 歌曲
     * @param alias 别名
     * @param openId 投票用户的 OpenID
     * @return 是否已投票
     */
    suspend operator fun get(
        music: MusicInfo,
        alias: String,
        openId: String
    ) = suspendedTransactionAsync {
        selectAll().where {
            (ChunithmMusicAliasesVoteTable.id eq music.id) and (name eq alias) and (user eq openId)
        }.count() > 0
    }.await()
}