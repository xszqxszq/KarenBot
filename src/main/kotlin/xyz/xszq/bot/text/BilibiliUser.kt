package xyz.xszq.bot.text

import kotlinx.serialization.Serializable

@Serializable
data class BilibiliUser(
    val mid: Long,
    val name: String,
    val face: String
)