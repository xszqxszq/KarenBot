package xyz.xszq.bot.chunithm.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import xyz.xszq.bot.chunithm.music.PlayerSettings

/**
 * 用户自定义设置表
 */
@Suppress("unused")
object MaimaiSettingsTable: Table() {
    val id = varchar("id", 32)
    val key = varchar("key", 32)
    val value = varchar("value", 512)
    override val primaryKey = PrimaryKey(id, key)

    /**
     * 修改设置里对应键的值
     *
     * @param openId OpenID
     * @param key 设置键
     * @param value 设置值
     */
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
    /**
     * 读取设置里键的值
     *
     * @param openId OpenID
     * @param key 设置键
     * @return 设置值
     */
    suspend operator fun get(
        openId: String,
        key: String
    ) = suspendedTransactionAsync {
        select(value).where {
            (MaimaiSettingsTable.id eq openId) and (MaimaiSettingsTable.key eq key)
        }.map { it[value] }.firstOrNull()?.let { it.ifBlank { null } }
    }.await()

    /**
     * 读取玩家自定义的头像与底板设置
     *
     * @param openId OpenID
     * @return 玩家设置
     */
    suspend fun settings(openId: String) = suspendedTransactionAsync {
        val rows = selectAll().where { MaimaiSettingsTable.id eq openId }
        val map = rows.associate { it[key] to it[value] }
        PlayerSettings(
            avatar = map["icon"]?.ifBlank { null }?.toIntOrNull(),
            plate = map["plate"]?.ifBlank { null }?.toIntOrNull()
        )
    }.await()

    /**
     * 读取玩家默认查询的游戏
     *
     * @param openId OpenID
     * @return 游戏名
     */
    suspend fun defaultGame(
        openId: String
    ): String {
        return MaimaiSettingsTable[openId, "game-prior"] ?: "maimai"
    }

    /**
     * 设置玩家默认查询的游戏
     *
     * @param openId OpenID
     * @param game 游戏名
     */
    suspend fun setDefaultGame(
        openId: String,
        game: String
    ) {
        MaimaiSettingsTable[openId, "game-prior"] = game
    }
}