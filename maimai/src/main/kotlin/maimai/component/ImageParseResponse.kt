package xyz.xszq.bot.maimai.component

import kotlinx.serialization.Serializable

@Serializable
data class ImageParseResponse(
    val results: List<ImageParseResult>,
)
