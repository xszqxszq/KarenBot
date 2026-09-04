package xyz.xszq.bot.payload.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 向量化请求 Token 使用量
 */
@Serializable
data class EmbeddingUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("total_tokens")
    val totalTokens: Int = 0
)