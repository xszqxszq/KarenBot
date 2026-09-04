package xyz.xszq.bot.payload.llm

import kotlinx.serialization.Serializable

/**
 * 向量化结果
 */
@Serializable
data class EmbeddingData(
    val embedding: List<Float> = emptyList()
)