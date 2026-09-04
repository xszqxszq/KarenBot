package xyz.xszq.bot.config

import kotlinx.serialization.Serializable

/**
 * Webhook 事件转发配置
 */
@Serializable
data class ForwardConfig(
    val whitelist: String = "",
    val otherwise: String = "",
    val subjects: List<String> = listOf(),
    val groups: List<String> = listOf()
)