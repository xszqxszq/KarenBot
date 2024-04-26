package xyz.xszq.karenbot.ffmpeg

import kotlinx.serialization.Serializable

@Serializable
data class FFProbeFormat(
    val filename: String,
    val duration: String
)
