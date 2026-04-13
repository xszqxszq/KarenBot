package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LXNSAlias(
    @SerialName("song_id")
    val songId: Int,
    val aliases: List<String>
)