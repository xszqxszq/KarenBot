package xyz.xszq.bot.config

import kotlinx.serialization.Serializable

@Serializable
data class CosConfig(
    val appId: String = "",
    val region: String = "",
    val secretId: String = "",
    val secretKey: String = "",
    val lightMode: Boolean = false,
    val lightDir: String = "",
    val lightUrl: String = ""
)
