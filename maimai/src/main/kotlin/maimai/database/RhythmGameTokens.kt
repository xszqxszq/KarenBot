package xyz.xszq.bot.maimai.database

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import xyz.xszq.bot.database.newSuspendedTransaction
import xyz.xszq.bot.event.GroupMessageEvent
import xyz.xszq.bot.event.MessageEvent

/**
 * 查分器进行绑定操作时的令牌表
 *
 * 写入与删除直接操作本表，读取经 DAO `RhythmGameToken` 进行
 */
object RhythmGameTokens: IdTable<String>() {
    override val id = varchar("id", 64).entityId()
    val eventType = varchar("event_type", 5)
    val eventId = varchar("event_id", 256)
    val messageId = varchar("message_id", 256)
    val message = text("message")
    val senderId = varchar("sender_id", 32)
    val groupId = varchar("group_id", 32).nullable()
    val seq = integer("seq")
    val replay = bool("replay")
    val expiresAt = long("expires_at")

    /**
     * 保存令牌及其对应的消息事件
     *
     * 已存在同 id 记录时先删除再写入
     *
     * @param id 令牌
     * @param event 消息事件
     * @param replay 授权完成后是否重放原消息
     * @param expiresAt 过期时间
     */
    suspend fun save(
        id: String,
        event: MessageEvent,
        replay: Boolean,
        expiresAt: Long
    ) = newSuspendedTransaction {
        deleteWhere { RhythmGameTokens.id eq id }
        insert {
            it[RhythmGameTokens.id] = id
            it[RhythmGameTokens.eventType] = if (event is GroupMessageEvent) "group" else "c2c"
            it[RhythmGameTokens.eventId] = event.eventId
            it[RhythmGameTokens.messageId] = event.id
            it[RhythmGameTokens.message] = event.text
            it[RhythmGameTokens.senderId] = event.sender.id
            it[RhythmGameTokens.groupId] = (event as? GroupMessageEvent)?.group?.id
            it[RhythmGameTokens.seq] = event.seq
            it[RhythmGameTokens.replay] = replay
            it[RhythmGameTokens.expiresAt] = expiresAt
        }
    }

    /**
     * 删除指定令牌记录
     *
     * @param id 令牌
     */
    suspend fun remove(id: String) = newSuspendedTransaction {
        deleteWhere { RhythmGameTokens.id eq id }
    }
}