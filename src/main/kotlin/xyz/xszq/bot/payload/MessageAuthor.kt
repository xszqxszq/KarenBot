package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

/**
 * 引用消息中的作者信息
 */
@Serializable
data class MessageAuthor(
    val username: String,
    val bot: Boolean,
)