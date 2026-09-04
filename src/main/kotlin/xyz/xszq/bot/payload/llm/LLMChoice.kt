package xyz.xszq.bot.payload.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * LLM 文本响应的候选项
 */
@Serializable
data class LLMChoice(
    val index: Int,
    val message: LLMMessage,
    @SerialName("finish_reason")
    val finishReason: String,
)