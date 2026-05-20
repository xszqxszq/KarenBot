package xyz.xszq.bot.maimai.database

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.json.jsonb

object GuessGameTable: IdTable<String>() {
    override val id = varchar("context", 32).entityId()
    val eventType = varchar("event_type", 5)
    val eventId = varchar("event_id", 256)
    val messageId = varchar("message_id", 256)
    val senderId = varchar("sender_id", 32)
    val seq = integer("seq")
    val type = varchar("type", 32)
    val status = jsonb<GuessGameStatus>("status", Json, GuessGameStatus.serializer())
    override val primaryKey = PrimaryKey(id)
}