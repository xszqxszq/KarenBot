package xyz.xszq.bot.chunithm.config

import kotlinx.serialization.Serializable

@Serializable
data class ChunithmConfig(
    val database: DatabaseConfig,
    val tokens: Map<String, String> = emptyMap()
)