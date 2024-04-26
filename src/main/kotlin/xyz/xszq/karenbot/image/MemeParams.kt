package xyz.xszq.karenbot.image

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MemeParams(
    @SerialName("min_images")
    val minImages: Int,
    @SerialName("max_images")
    val maxImages: Int,
    @SerialName("min_texts")
    val minTexts: Int,
    @SerialName("max_texts")
    val maxTexts: Int,
    @SerialName("default_texts")
    val defaultTexts: List<String>,
    val args: List<MemeArgument>
)