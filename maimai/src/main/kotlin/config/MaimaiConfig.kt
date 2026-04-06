package xyz.xszq.bot.config

import kotlinx.serialization.Serializable

@Serializable
data class MaimaiConfig(
    val database: DatabaseConfig,
    val tokens: Map<String, String>,
    val tips: List<String>
)
