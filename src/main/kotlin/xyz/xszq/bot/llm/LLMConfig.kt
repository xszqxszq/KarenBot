package xyz.xszq.bot.llm

import kotlinx.serialization.Serializable

@Serializable
data class LLMConfig(
    val apikey: String,
    val url: String,
    val model: String,
    val temperature: Double = 0.1,
)
