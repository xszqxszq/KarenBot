package xyz.xszq.bot.payload.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 向量化输入的内容
 */
@Serializable
data class EmbeddingContentPart(
    val type: String,
    val text: String ?= null,
    @SerialName("image_url")
    val imageUrl: EmbeddingImageUrl ?= null
)