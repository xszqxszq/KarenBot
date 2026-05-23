package xyz.xszq.bot.config

import kotlinx.serialization.Serializable

@Serializable
data class TextConfig(
    val system: String = "",
    val presets: Map<String, String> = emptyMap(),
    val remoteApi: String? = null,
    val remoteKey: String,
)
