package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class MessageResponse(
    val id: String,
    val timestamp: String
)