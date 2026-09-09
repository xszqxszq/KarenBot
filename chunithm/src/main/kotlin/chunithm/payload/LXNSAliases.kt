package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

/**
 * 落雪查分器的别名列表
 */
@Serializable
data class LXNSAliases(
    val aliases: List<LXNSAlias>
)