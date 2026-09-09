package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 落雪查分器的歌曲别名
 *
 * @property aliases 该歌曲的全部搜索别名
 */
@Serializable
data class LXNSAlias(
    @SerialName("song_id")
    val songId: Int,
    val aliases: List<String>
)