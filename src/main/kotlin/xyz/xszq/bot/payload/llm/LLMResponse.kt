package xyz.xszq.bot.payload.llm

import kotlinx.serialization.Serializable

/**
 * LLM 文本响应
 */
@Serializable
data class LLMResponse(
    val id: String,
    val created: Long,
    val model: String,
    val choices: List<LLMChoice>,
)