package xyz.xszq.bot.feedback

import kotlinx.datetime.LocalDateTime

/**
 * 反馈工单
 */
data class FeedbackTicket(
    val id: String,
    val type: String,
    val status: String,
    val body: String,
    val email: String,
    val game: String,
    val songId: Int,
    val alias: String,
    val userId: String,
    val userName: String,
    val groupId: String,
    val createdAt: LocalDateTime,
    val handledAt: LocalDateTime ?= null,
    val reply: String,
    val emailMessageId: String ?= null
)