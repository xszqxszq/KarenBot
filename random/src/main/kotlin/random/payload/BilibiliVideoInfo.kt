package xyz.xszq.bot.random.payload

import kotlinx.serialization.Serializable

/**
 * B站视频信息
 */
@Serializable
data class BilibiliVideoInfo(
    val bvid: String,
    val aid: Long,
    val title: String,
    val pic: String,
    val desc: String,
    val owner: BilibiliVideoOwner,
)