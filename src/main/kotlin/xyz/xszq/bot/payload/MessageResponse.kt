package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

/**
 * 发送消息的响应
 */
@Serializable
data class MessageResponse(
    val id: String,
    val timestamp: String
)