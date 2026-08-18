package xyz.xszq.bot.text.payload

import kotlinx.serialization.Serializable

@Serializable
data class BilibiliVideoOwner(
    val mid: Long,
    val name: String
)
