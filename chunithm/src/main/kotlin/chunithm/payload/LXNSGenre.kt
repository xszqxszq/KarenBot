package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

/**
 * 落雪查分器的歌曲
 */
@Serializable
data class LXNSGenre(
    val id: Int,
    val genre: String
)