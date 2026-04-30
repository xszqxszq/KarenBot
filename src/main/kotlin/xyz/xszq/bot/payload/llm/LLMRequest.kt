package xyz.xszq.bot.payload.llm

import kotlinx.serialization.Serializable

@Serializable
data class LLMRequest(
    val model: String,
    val messages: List<LLMMessage>,
    val stream: Boolean = false,
    val temperature: Double,
    val thinking: LLMThinking? = null,
)
