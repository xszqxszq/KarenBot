package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

@Serializable
data class LXNSSongs(
    val songs: List<LXNSSong>,
    val genres: List<LXNSGenre>,
    val versions: List<LXNSVersion>
)