package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

/**
 * 落雪歌曲信息
 */
@Serializable
data class LXNSSongs(
    val songs: List<LXNSSong>,
    val genres: List<LXNSGenre>,
    val versions: List<LXNSVersion>
)