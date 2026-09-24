package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

/**
 * 落雪查分器的牌子
 */
@Serializable
data class LXNSPlateInfo(
    val id: Int,
    val name: String,
    val description: String ?= null
)