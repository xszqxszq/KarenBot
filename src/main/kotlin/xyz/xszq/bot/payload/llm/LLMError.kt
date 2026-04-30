package xyz.xszq.bot.payload.llm

import kotlinx.serialization.Serializable

@Serializable
data class LLMError(
    val message: String,
    val type: String? = null,
    val code: String? = null,
)
