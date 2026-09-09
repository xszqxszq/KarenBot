package xyz.xszq.bot.meme.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 表情包模板的参数要求
 *
 * @property defaultTexts 默认文本
 * @property options 模板可选参数
 */
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
    val options: List<MemeOption>
)