package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

/**
 * 落雪查分器的游戏版本
 */
@Serializable
data class LXNSVersion(
    val id: Int,
    val title: String,
    val version: Int
)