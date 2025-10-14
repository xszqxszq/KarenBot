package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class MaimaiPlayerInfo(
    val userName: String,
    val playerRating: Int,
    val iconId: Int
)
