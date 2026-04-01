package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class LLMContextCreateRequest(
    val model: String,
    val messages: List<LLMMessage>,
    val ttl: Int,
    val mode: String
)
