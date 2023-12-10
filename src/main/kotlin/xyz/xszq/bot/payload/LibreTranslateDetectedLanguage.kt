package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class LibreTranslateDetectedLanguage(
    val confidence: Double,
    val language: String
)
