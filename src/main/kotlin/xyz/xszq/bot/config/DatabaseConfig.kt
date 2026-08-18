package xyz.xszq.bot.config

import kotlinx.serialization.Serializable

@Serializable
data class DatabaseConfig(
    val url: String,
    val driver: String,
    val username: String,
    val password: String
)