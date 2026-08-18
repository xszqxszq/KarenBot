package xyz.xszq.bot.llm

import kotlinx.serialization.Serializable

@Serializable
data class LLMConfig(
    val models: Map<String, LLMModelConfig>,
)