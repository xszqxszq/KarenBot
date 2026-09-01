package xyz.xszq.bot.maimai.database

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import xyz.xszq.bot.event.GroupMessageEvent
import xyz.xszq.bot.event.MessageEvent

object RhythmGameTokens: Table() {
    data class Row(
        val id: String,
        val eventType: String,
        val eventId: String,
        val messageId: String,
        val message: String,
        val senderId: String,
        val groupId: String ?= null,
        val seq: Int,
        val replay: Boolean,
        val expiresAt: Long
    )

    val id = varchar("id", 64)
    val eventType = varchar("event_type", 5)
    val eventId = varchar("event_id", 256)
    val messageId = varchar("message_id", 256)
    val message = text("message")
    val senderId = varchar("sender_id", 32)
    val groupId = varchar("group_id", 32).nullable()
    val seq = integer("seq")
    val replay = bool("replay")
    val expiresAt = long("expires_at")
    override val primaryKey = PrimaryKey(id)
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
    suspend fun load(): List<Row> = suspendedTransactionAsync {
        selectAll().map { row ->
            Row(
                id = row[RhythmGameTokens.id],
                eventType = row[RhythmGameTokens.eventType],
                eventId = row[RhythmGameTokens.eventId],
                messageId = row[RhythmGameTokens.messageId],
                message = row[RhythmGameTokens.message],
                senderId = row[RhythmGameTokens.senderId],
                groupId = row[RhythmGameTokens.groupId],
                seq = row[RhythmGameTokens.seq],
                replay = row[RhythmGameTokens.replay],
                expiresAt = row[RhythmGameTokens.expiresAt]
            )
        }
    }.await()
    suspend fun remove(id: String) = newSuspendedTransaction {
        deleteWhere { RhythmGameTokens.id eq id }
    }
}
