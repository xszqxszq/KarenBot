package xyz.xszq.bot.maimai.component

import kotlinx.serialization.Serializable

/**
 * 图片解析响应
 */
@Serializable
data class ImageParseResponse(
    val results: List<ImageParseResult>,
)