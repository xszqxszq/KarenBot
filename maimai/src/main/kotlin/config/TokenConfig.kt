package xyz.xszq.bot.config

import kotlinx.serialization.Serializable

@Serializable
data class TokenConfig(
    val tokens: Map<String, String>
)
