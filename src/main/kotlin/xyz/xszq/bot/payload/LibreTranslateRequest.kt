package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class LibreTranslateRequest(
    val q: String,
    val source: String,
    val target: String,
    val format: String,
    val apiKey: String = ""
)
