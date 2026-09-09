package xyz.xszq.bot.maimai.config

import kotlinx.serialization.Serializable

/**
 * 舞萌插件配置
 *
 * @param apiServer API 服务监听地址
 * @param tokens 各种 Token
 * @param tips 随机提示文本
 */
@Serializable
data class MaimaiConfig(
    val apiServer: String = "http://localhost:18100",
    val tokens: Map<String, String>,
    val tips: List<String>
)