package xyz.xszq.bot.feedback.database

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import xyz.xszq.bot.database.newSuspendedTransaction
import xyz.xszq.bot.feedback.FeedbackTicket
import java.util.UUID

/**
 * 反馈工单表
 */
object FeedbackTicketTable: Table() {
    val id = varchar("id", 36)
    val type = varchar("type", 16)
    val status = varchar("status", 16)
    val body = text("body")
    val email = varchar("email", 254)
    val game = varchar("game", 32)
    val songId = integer("song_id")
    val alias = varchar("alias", 128)
    val userId = varchar("user_id", 32)
    val userName = varchar("user_name", 128)
    val groupId = varchar("group_id", 32)
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }
    val handledAt = datetime("handled_at").nullable()
    val reply = text("reply")
    val emailMessageId = varchar("email_message_id", 254).nullable()
    override val primaryKey = PrimaryKey(id)

    suspend fun load(
        db: Database,
        id: String
    ) = newSuspendedTransaction(db = db) {
        selectAll().where { FeedbackTicketTable.id eq id }
            .map(::toTicket)
            .firstOrNull()
    }

    suspend fun page(
        db: Database,
        status: String,
        offset: Int,
        limit: Int
    ) = newSuspendedTransaction(db = db) {
        val query = if (status.isBlank())
            selectAll()
        else
            selectAll().where { FeedbackTicketTable.status eq status }
        query.orderBy(createdAt, SortOrder.DESC)
            .limit(limit).offset(offset.toLong())
            .map(::toTicket)
    }

    suspend fun count(
        db: Database,
        status: String = ""
    ) = newSuspendedTransaction(db = db) {
        if (status.isBlank())
            selectAll().count()
        else
            selectAll().where { FeedbackTicketTable.status eq status }.count()
    }

    suspend fun ofUser(
        db: Database,
        userId: String,
        limit: Int
    ) = newSuspendedTransaction(db = db) {
        selectAll().where { FeedbackTicketTable.userId eq userId }
            .orderBy(createdAt, SortOrder.DESC)
            .limit(limit)
            .map(::toTicket)
    }

    suspend fun openCount(db: Database) = count(db, "open")

    // 每个用户最近一次填的邮箱，旧值先放后放都能被最新一条覆盖
    suspend fun latestEmails(db: Database) = newSuspendedTransaction(db = db) {
        selectAll()
            .where { FeedbackTicketTable.email neq "" }
            .orderBy(createdAt, SortOrder.ASC)
            .map { it[userId] to it[email] }
            .toMap()
    }

    suspend fun insert(
        db: Database,
        type: String,
        body: String,
        userId: String,
        userName: String,
        game: String = "",
        songId: Int = 0,
        alias: String = "",
        groupId: String = "",
        email: String = ""
    ) = newSuspendedTransaction(db = db) {
        val id = UUID.randomUUID().toString()
        FeedbackTicketTable.insert {
            it[FeedbackTicketTable.id] = id
            it[FeedbackTicketTable.type] = type
            it[status] = "open"
            it[FeedbackTicketTable.body] = body
            it[FeedbackTicketTable.email] = email
            it[FeedbackTicketTable.game] = game
            it[FeedbackTicketTable.songId] = songId
            it[FeedbackTicketTable.alias] = alias
            it[FeedbackTicketTable.userId] = userId
            it[FeedbackTicketTable.userName] = userName
            it[FeedbackTicketTable.groupId] = groupId
            it[reply] = ""
        }
        id
    }

    suspend fun updateReply(
        db: Database,
        id: String,
        reply: String,
        status: String
    ) = newSuspendedTransaction(db = db) {
        FeedbackTicketTable.update({ FeedbackTicketTable.id eq id }) {
            it[FeedbackTicketTable.reply] = reply
            it[FeedbackTicketTable.status] = status
            it[handledAt] = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
        }
    }

    suspend fun updateEmail(
        db: Database,
        userId: String,
        email: String
    ) = newSuspendedTransaction(db = db) {
        FeedbackTicketTable.update({ FeedbackTicketTable.userId eq userId }) {
            it[FeedbackTicketTable.email] = email
            it[emailMessageId] = null
        }
    }

    suspend fun updateEmailMessageId(
        db: Database,
        id: String,
        messageId: String
    ) = newSuspendedTransaction(db = db) {
        FeedbackTicketTable.update({ FeedbackTicketTable.id eq id }) {
            it[emailMessageId] = messageId
        }
    }

    private fun toTicket(row: ResultRow) = FeedbackTicket(
        id = row[id],
        type = row[type],
        status = row[status],
        body = row[body],
        email = row[email],
        game = row[game],
        songId = row[songId],
        alias = row[alias],
        userId = row[userId],
        userName = row[userName],
        groupId = row[groupId],
        createdAt = row[createdAt],
        handledAt = row[handledAt],
        reply = row[reply],
        emailMessageId = row[emailMessageId]
    )
}