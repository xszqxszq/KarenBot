package xyz.xszq.bot.text.payload

import kotlinx.serialization.Serializable

@Serializable
data class BilibiliHtmlData(
    val videoData: BilibiliVideoInfo
)
