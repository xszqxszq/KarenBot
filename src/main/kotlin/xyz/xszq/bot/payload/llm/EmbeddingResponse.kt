package xyz.xszq.bot.payload.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmbeddingResponse(
    val created: Long? = null,
    val data: EmbeddingData? = null,
    val model: String? = null,
    val usage: EmbeddingUsage? = null,
)

@Serializable
data class EmbeddingData(
    val embedding: List<Float> = emptyList(),
)

@Serializable
data class EmbeddingUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("total_tokens")
    val totalTokens: Int = 0,
)
