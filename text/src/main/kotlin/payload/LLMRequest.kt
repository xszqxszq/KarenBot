package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class LLMRequest(
    val model: String,
    val messages: List<LLMMessage>,
    val stream: Boolean,
    val temperature: Double
)
