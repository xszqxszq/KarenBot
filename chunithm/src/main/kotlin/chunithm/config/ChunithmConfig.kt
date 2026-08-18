package xyz.xszq.bot.chunithm.config

import kotlinx.serialization.Serializable

@Serializable
data class ChunithmConfig(
    val tokens: Map<String, String> = emptyMap()
)