package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

@Serializable
data class LXNSGenre(
    val id: Int,
    val genre: String
)
