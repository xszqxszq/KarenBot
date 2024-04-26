package xyz.xszq.otomadbot.image

import kotlinx.serialization.Serializable

@Serializable
data class Meme(
    val key: String,
    val keywords: List<String>,
    val patterns: List<String>,
    val params: MemeParams
)