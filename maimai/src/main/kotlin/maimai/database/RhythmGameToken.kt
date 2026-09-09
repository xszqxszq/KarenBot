package xyz.xszq.bot.maimai.database

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * 查分器进行绑定操作时的令牌记录
 */
class RhythmGameToken(id: EntityID<String>): Entity<String>(id) {
    var eventType by RhythmGameTokens.eventType
    var eventId by RhythmGameTokens.eventId
    var messageId by RhythmGameTokens.messageId
    var message by RhythmGameTokens.message
    var senderId by RhythmGameTokens.senderId
    var groupId by RhythmGameTokens.groupId
    var seq by RhythmGameTokens.seq
    var replay by RhythmGameTokens.replay
    var expiresAt by RhythmGameTokens.expiresAt

    companion object : EntityClass<String, RhythmGameToken>(RhythmGameTokens) {
        /**
         * 读取全部令牌记录
         *
         * @return 令牌记录列表
         */
        suspend fun load(): List<RhythmGameToken> = newSuspendedTransaction {
            all().toList()
        }
    }
}