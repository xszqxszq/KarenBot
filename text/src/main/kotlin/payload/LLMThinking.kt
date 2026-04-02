package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class LLMThinking(
    val type: String
)
