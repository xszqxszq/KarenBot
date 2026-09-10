package xyz.xszq.bot.chunithm.config

import kotlinx.serialization.Serializable

/**
 * Chunithm 插件配置
 *
 * @param tokens 各种 Token
 */
@Serializable
data class ChunithmConfig(
    val tokens: Map<String, String> = emptyMap()
)