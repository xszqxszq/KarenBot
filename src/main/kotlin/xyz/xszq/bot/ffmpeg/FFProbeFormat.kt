package xyz.xszq.bot.ffmpeg

import kotlinx.serialization.Serializable

@Serializable
data class FFProbeFormat(
    val filename: String,
    val duration: String
)
