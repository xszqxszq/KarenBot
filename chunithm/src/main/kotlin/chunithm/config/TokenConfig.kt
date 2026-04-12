package xyz.xszq.bot.chunithm.config

import kotlinx.serialization.Serializable

/**
 * 开发者Token配置文件
 */
@Serializable
data class TokenConfig(
    val tokens: Map<String, String>
)
