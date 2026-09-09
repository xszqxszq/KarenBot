package xyz.xszq.bot.meme.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 生成结果响应
 *
 * @property id 图片 ID
 */
@Serializable
data class MemeImageId(
    @SerialName("image_id")
    val id: String
)