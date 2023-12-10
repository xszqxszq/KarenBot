package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class LibreTranslateResponse(
    val translatedText: String,
    val detectedLanguage: LibreTranslateDetectedLanguage? = null
)
