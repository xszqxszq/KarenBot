package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

@Serializable
data class LXNSTrophyRequiredSong(
    val id: Int,
    val title: String
)
