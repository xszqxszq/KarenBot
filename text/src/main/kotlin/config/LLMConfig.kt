package xyz.xszq.bot.config

import kotlinx.serialization.Serializable

@Serializable
data class LLMConfig(
    val apikey: String,
    val url: String,
    val model: String,
    val system: String,
    val temperature: Double,
)
