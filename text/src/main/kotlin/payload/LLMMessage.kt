package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class LLMMessage(
    val role: String,
    val content: String
)
