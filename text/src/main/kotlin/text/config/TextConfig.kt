package xyz.xszq.bot.text.config

import kotlinx.serialization.Serializable

/**
 * 文本插件配置
 */
@Serializable
data class TextConfig(
    val system: String = "",
    val presets: Map<String, String> = emptyMap(),
    val userSpecifiedPresets: List<UserSpecifiedPreset> = emptyList(),
)