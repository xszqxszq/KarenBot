package xyz.xszq.bot.payload.llm

import kotlinx.serialization.Serializable

/**
 * 向量化响应
 *
 * @property data 向量
 * @property usage Token 消耗量
 */
@Serializable
data class EmbeddingResponse(
    val created: Long ?= null,
    val data: EmbeddingData ?= null,
    val model: String ?= null,
    val usage: EmbeddingUsage ?= null
)