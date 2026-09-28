package xyz.xszq.bot.payload.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 多模态文件
 */
@Serializable
data class ContentPart(
    val type: String,
    val text: String ?= null,
    @SerialName("image_url")
    val imageUrl: ImageUrl ?= null,
    @SerialName("input_audio")
    val inputAudio: InputAudio ?= null,
)