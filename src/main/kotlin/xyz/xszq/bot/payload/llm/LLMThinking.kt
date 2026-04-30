package xyz.xszq.bot.payload.llm

import kotlinx.serialization.Serializable

@Serializable
data class LLMThinking(
    val type: String,
)
