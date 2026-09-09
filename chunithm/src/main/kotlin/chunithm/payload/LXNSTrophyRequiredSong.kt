package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

/**
 * 落雪查分器的称号达成条件的歌曲
 */
@Serializable
data class LXNSTrophyRequiredSong(
    val id: Int,
    val title: String
)