package xyz.xszq.bot.text.config

import kotlinx.serialization.Serializable

/**
 * 自定义文本回复预设
 */
@Serializable
data class UserSpecifiedPreset(
    val openId: String,
    val match: String,
    val reply: String,
)