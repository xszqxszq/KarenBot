package xyz.xszq.bot.payload.llm

import kotlinx.serialization.Serializable

@Serializable
data class LLMResponse(
    val id: String,
    val created: Long,
    val model: String,
    val choices: List<LLMChoice>,
)
