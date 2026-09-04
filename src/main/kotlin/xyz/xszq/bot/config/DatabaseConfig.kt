package xyz.xszq.bot.config

import kotlinx.serialization.Serializable

/**
 * 数据库连接配置
 *
 * 支持 H2 / MySQL
 */
@Serializable
data class DatabaseConfig(
    val url: String,
    val driver: String,
    val username: String,
    val password: String
)