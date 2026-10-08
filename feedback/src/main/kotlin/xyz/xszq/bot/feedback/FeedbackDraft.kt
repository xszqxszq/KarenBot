package xyz.xszq.bot.feedback

import kotlinx.datetime.LocalDateTime

/**
 * 反馈填写草稿
 */
data class FeedbackDraft(
    val userId: String,
    val userName: String,
    val status: String,
    val feedbackId: String,
    val groupId: String,
    val createdAt: LocalDateTime
)