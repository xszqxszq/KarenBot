package xyz.xszq.bot.feedback.database

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import xyz.xszq.bot.database.newSuspendedTransaction

/**
 * 反馈接收邮箱表
 */
object FeedbackEmailTable: Table() {
    val userId = varchar("user_id", 32)
    val email = varchar("email", 254)
    val updatedAt = datetime("updated_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }
    override val primaryKey = PrimaryKey(userId)

    suspend fun save(
        db: Database,
        userId: String,
        email: String
    ) = newSuspendedTransaction(db = db) {
        val match = FeedbackEmailTable.userId eq userId
        val now = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
        if (selectAll().where { match }.count() != 0L)
            update({ match }) {
                it[FeedbackEmailTable.email] = email
                it[updatedAt] = now
            }
        else
            insert {
                it[FeedbackEmailTable.userId] = userId
                it[FeedbackEmailTable.email] = email
            }
    }

    suspend fun load(
        db: Database,
        userId: String
    ) = newSuspendedTransaction(db = db) {
        selectAll().where { FeedbackEmailTable.userId eq userId }
            .firstOrNull()
            ?.get(email)
    }
}