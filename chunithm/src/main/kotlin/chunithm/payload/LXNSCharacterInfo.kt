package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

/**
 * 落雪查分器的角色
 */
@Serializable
data class LXNSCharacterInfo(
    val id: Int,
    val name: String,
    val description: String ?= null
)