package xyz.xszq.bot.config

import kotlinx.serialization.Serializable

@Serializable
data class BotConfig(
    val qq: Long = 0,
    val appId: String = "",
    val token: String = "",
    val clientSecret: String = "",
    val port: Int = 18080,
    val forward: Boolean = false,
    val database: DatabaseConfig
)
