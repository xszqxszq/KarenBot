package xyz.xszq.bot.meme

import kotlinx.serialization.Serializable

/**
 * 表情包插件配置
 *
 * @param server meme-generator 服务器地址
 */
@Serializable
data class MemeConfig(
    val server: String
)