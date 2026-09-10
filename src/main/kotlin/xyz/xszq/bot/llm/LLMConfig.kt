package xyz.xszq.bot.llm

import kotlinx.serialization.Serializable

/**
 * LLM 配置
 */
@Serializable
@Suppress("unused")
data class LLMConfig(
    val models: Map<String, LLMModelConfig>,
)