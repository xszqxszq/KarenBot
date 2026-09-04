package xyz.xszq.bot.payload.llm

import kotlinx.serialization.Serializable

/**
 * LLM 思考类型
 */
@Serializable
data class LLMThinking(
    val type: String,
)