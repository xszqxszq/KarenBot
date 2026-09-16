package xyz.xszq.bot.chunithm.database

import org.jetbrains.exposed.sql.*
import xyz.xszq.bot.chunithm.music.PlayerSettings
import xyz.xszq.bot.database.newSuspendedTransaction
import xyz.xszq.bot.database.suspendedTransactionAsync
import java.util.concurrent.ConcurrentHashMap

/**
 * 用户自定义设置表
 */
@Suppress("unused")
object MaimaiSettingsTable: Table() {
    val id = varchar("id", 32)
    val key = varchar("key", 32)
    val value = varchar("value", 512)
    override val primaryKey = PrimaryKey(id, key)

    // 缓存变动广播频道
    const val CACHE_CHANNEL = "maimai-settings"

    // 缓存变动广播
    var publisher: suspend (String) -> Unit = {}

    private val cache = ConcurrentHashMap<String, Map<String, String>>()
    private val generations = ConcurrentHashMap<String, Long>()

    /**
     * 清除该用户缓存
     *
     * @param openId OpenID
     */
    fun clearCache(openId: String) {
        generations.merge(openId, 1L) { current, _ -> current + 1 }
        cache.remove(openId)
    }

    /**
     * 清空全部缓存
     */
    fun clearCache() {
        cache.clear()
        generations.clear()
    }

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
    }.also {
        clearCache(openId)
        publisher(openId)
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
    ): String? = rows(openId)[key] ?.ifBlank { null }

    /**
     * 读取玩家自定义的头像与底板设置
     *
     * @param openId OpenID
     * @return 玩家设置
     */
    suspend fun settings(openId: String): PlayerSettings = rows(openId).let { rows ->
        PlayerSettings(
            avatar = rows["icon"] ?.ifBlank { null } ?.toIntOrNull(),
            plate = rows["plate"] ?.ifBlank { null } ?.toIntOrNull()
        )
    }

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

    /**
     * 读取用户的全部设置
     *
     * @param openId OpenID
     * @return 全部设置
     */
    private suspend fun rows(openId: String): Map<String, String> {
        cache[openId] ?.let { return it }
        val generation = generations[openId] ?: 0L
        val loaded = suspendedTransactionAsync {
            selectAll().where { MaimaiSettingsTable.id eq openId }
                .associate { it[key] to it[value] }
        }.await()
        if ((generations[openId] ?: 0L) == generation)
            cache[openId] = loaded
        return loaded
    }
}