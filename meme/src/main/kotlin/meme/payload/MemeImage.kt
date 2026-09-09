package xyz.xszq.bot.meme.payload

import kotlinx.serialization.Serializable

/**
 * 生成请求中的图片引用
 *
 * @property id 图片 ID
 */
@Serializable
data class MemeImage(
    val name: String,
    val id: String
)