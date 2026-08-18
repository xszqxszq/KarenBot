package xyz.xszq.bot.text.payload

import kotlinx.serialization.Serializable

@Serializable
data class BilibiliVideoInfo(
    val bvid: String,
    val aid: Long,
    val title: String,
    val pic: String,
    val desc: String,
    val owner: BilibiliVideoOwner,
)
