package xyz.xszq.bot.random.payload

import kotlinx.serialization.Serializable

/**
 * B站UP主
 */
@Serializable
data class BilibiliVideoOwner(
    val mid: Long,
    val name: String
)