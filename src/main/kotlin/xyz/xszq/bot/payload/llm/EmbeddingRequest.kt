package xyz.xszq.bot.payload.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmbeddingRequest(
    val model: String,
    val input: List<EmbeddingContentPart>,
    @SerialName("encoding_format")
    val encodingFormat: String = "float",
)

@Serializable
data class EmbeddingContentPart(
    val type: String,
    val text: String? = null,
    @SerialName("image_url")
    val imageUrl: EmbeddingImageUrl? = null,
)

@Serializable
data class EmbeddingImageUrl(
    val url: String,
)
