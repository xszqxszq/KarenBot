package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class LLMErrorResponse(
    val error: LLMError
)
