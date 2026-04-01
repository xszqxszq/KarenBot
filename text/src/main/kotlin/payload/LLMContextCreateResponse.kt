package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class LLMContextCreateResponse(
    val id: String
)
