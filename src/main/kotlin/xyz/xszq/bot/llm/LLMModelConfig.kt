package xyz.xszq.bot.llm

import kotlinx.serialization.Serializable

/**
 * LLM 模型配置
 */
@Serializable
@Suppress("unused")
data class LLMModelConfig(
    val apikey: String,
    val url: String,
    val model: String,
    val temperature: Double = 0.1,
)