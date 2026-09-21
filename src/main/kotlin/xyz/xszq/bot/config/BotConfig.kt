package xyz.xszq.bot.config

import kotlinx.serialization.Serializable

/**
 * Bot 配置
 */
@Serializable
@Suppress("unused")
data class BotConfig(
    val qq: Long = 0,
    val appId: String = "",
    val token: String = "",
    val clientSecret: String = "",
    val port: Int = 18080,
    val metricsPort: Int = 18081,
    val forward: Boolean = false,
    val database: DatabaseConfig
)