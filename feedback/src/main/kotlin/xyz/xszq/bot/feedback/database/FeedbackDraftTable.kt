package xyz.xszq.bot.feedback.database

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import xyz.xszq.bot.database.newSuspendedTransaction
import xyz.xszq.bot.feedback.FeedbackDraft

/**
 * 反馈填写草稿表
 */
object FeedbackDraftTable: Table() {
    val userId = varchar("user_id", 32)
    val userName = varchar("user_name", 128)
    // 填写到哪一步了
    val status = varchar("status", 32)
    val feedbackId = varchar("feedback_id", 36)
    val groupId = varchar("group_id", 32)
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }
    override val primaryKey = PrimaryKey(userId)

    suspend fun save(
        db: Database,
        userId: String,
        userName: String,
        status: String,
        feedbackId: String = "",
        groupId: String = ""
    ) = newSuspendedTransaction(db = db) {
        val match = FeedbackDraftTable.userId eq userId
        if (selectAll().where { match }.count() != 0L)
            update({ match }) {
                it[FeedbackDraftTable.userName] = userName
                it[FeedbackDraftTable.status] = status
                it[FeedbackDraftTable.feedbackId] = feedbackId
                it[FeedbackDraftTable.groupId] = groupId
                it[createdAt] = Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
            }
        else
            insert {
                it[FeedbackDraftTable.userId] = userId
                it[FeedbackDraftTable.userName] = userName
                it[FeedbackDraftTable.status] = status
                it[FeedbackDraftTable.feedbackId] = feedbackId
                it[FeedbackDraftTable.groupId] = groupId
            }
    }

    suspend fun remove(
        db: Database,
        userId: String
    ) = newSuspendedTransaction(db = db) {
        FeedbackDraftTable.deleteWhere { FeedbackDraftTable.userId eq userId }
    }

    suspend fun active(
        db: Database,
        expireBefore: LocalDateTime
    ) = newSuspendedTransaction(db = db) {
        selectAll().where { createdAt greaterEq expireBefore }
            .map(::toDraft)
    }

    suspend fun purgeExpired(
        db: Database,
        expireBefore: LocalDateTime
    ) = newSuspendedTransaction(db = db) {
        FeedbackDraftTable.deleteWhere { createdAt less expireBefore }
    }

    private fun toDraft(row: ResultRow) = FeedbackDraft(
        userId = row[userId],
        userName = row[userName],
        status = row[status],
        feedbackId = row[feedbackId],
        groupId = row[groupId],
        createdAt = row[createdAt]
    )
}