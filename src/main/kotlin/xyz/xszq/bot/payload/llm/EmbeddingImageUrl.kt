package xyz.xszq.bot.payload.llm

import kotlinx.serialization.Serializable

/**
 * 图片地址类型的向量化输入
 */
@Serializable
data class EmbeddingImageUrl(
    val url: String
)