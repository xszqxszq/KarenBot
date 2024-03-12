package xyz.xszq.bot.rhythmgame.lxns.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Alias(
    @SerialName("song_id")
    val songId: Int,
    val aliases: List<String>
)
