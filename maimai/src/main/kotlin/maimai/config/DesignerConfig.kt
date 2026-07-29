package xyz.xszq.bot.maimai.config

import kotlinx.serialization.Serializable

@Serializable
data class DesignerConfig(
    val aliases: Map<String, List<String>>,
    val includes: Map<String, List<String>>,
    val collabs: Map<String, List<String>>
)
