package xyz.xszq.bot.payload.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 向量化请求
 *
 * @property input 待向量化的文本与图片列表
 */
@Serializable
data class EmbeddingRequest(
    val model: String,
    val input: List<EmbeddingContentPart>,
    @SerialName("encoding_format")
    val encodingFormat: String = "float"
)