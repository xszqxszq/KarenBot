package xyz.xszq.bot.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import xyz.xszq.bot.event.GroupMessageEvent

/**
 * 群聊命令设置
 */
object GroupCommandSettings: Table() {
    val id = varchar("id", 32)
    val command = varchar("command", 64)
    val key = varchar("key", 32)
    val value = varchar("value", 512)
    override val primaryKey = PrimaryKey(id, command, key)

    /**
     * 读取设置值
     *
     * @param group 群 OpenID
     * @param command 命令名
     * @param key 键
     * @return 值
     */
    suspend operator fun get(
        group: String,
        command: String,
        key: String = "enabled"
    ): String? = suspendedTransactionAsync {
        select(value).where {
            (GroupCommandSettings.id eq group) and
                (GroupCommandSettings.command eq command) and
                (GroupCommandSettings.key eq key)
        }.map { it[value] }.firstOrNull() ?.let { it.ifBlank { null } }
    }.await()

    /**
     * 写入值
     *
     * @param group 群 OpenID
     * @param command 命令名
     * @param key 键
     * @param value 值
     */
    suspend operator fun set(
        group: String,
        command: String,
        key: String,
        value: String
    ) = newSuspendedTransaction {
        val match = (GroupCommandSettings.id eq group) and
            (GroupCommandSettings.command eq command) and
            (GroupCommandSettings.key eq key)
        if (selectAll().where { match }.count() != 0L)
            update({ match }) {
                it[GroupCommandSettings.value] = value
            }
        else
            insert {
                it[GroupCommandSettings.id] = group
                it[GroupCommandSettings.command] = command
                it[GroupCommandSettings.key] = key
                it[GroupCommandSettings.value] = value
            }
    }

    /**
     * 读取命令是否启用
     *
     * @param group 群 OpenID
     * @param command 命令名
     * @return 是否启用
     */
    suspend fun isEnabled(
        group: String,
        command: String
    ): Boolean = get(group, command) != "false"

    /**
     * 设置命令启用状态
     *
     * @param group 群 OpenID
     * @param command 命令名
     * @param enabled 是否启用
     */
    suspend fun setEnabled(
        group: String,
        command: String,
        enabled: Boolean
    ) = set(group, command, "enabled", enabled.toString())

    /**
     * 当命令未启用时
     *
     * @param enabled 命令是否启用
     * @param event 群消息事件
     */
    class WhenEnabled(
        private val enabled: Boolean,
        private val event: GroupMessageEvent
    ) {
        /**
         * 未启用时执行
         *
         * @param block 代码块
         */
        suspend fun orElse(block: suspend GroupMessageEvent.() -> Unit) {
            if (!enabled)
                block(event)
        }
    }
}
