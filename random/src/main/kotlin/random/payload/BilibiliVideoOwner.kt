package xyz.xszq.bot.random.payload

import kotlinx.serialization.Serializable

@Serializable
data class BilibiliVideoOwner(
    val mid: Long,
    val name: String
)