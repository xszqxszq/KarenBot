package xyz.xszq.bot.maimai.config

import kotlinx.serialization.Serializable

@Serializable
data class MaimaiConfig(
    val apiServer: String = "http://localhost:18100",
    val tokens: Map<String, String>,
    val tips: List<String>
)