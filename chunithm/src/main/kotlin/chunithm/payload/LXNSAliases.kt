package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

@Serializable
data class LXNSAliases(
    val aliases: List<LXNSAlias>
)