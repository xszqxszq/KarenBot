package xyz.xszq.bot.payload.llm

import kotlinx.serialization.Serializable

/**
 * 图片消息内容中的 URL
 */
@Serializable
data class ImageUrl(
    val url: String,
    val detail: String? = null,
)