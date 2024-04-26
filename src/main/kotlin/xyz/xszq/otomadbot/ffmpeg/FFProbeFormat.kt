package xyz.xszq.otomadbot.ffmpeg

import kotlinx.serialization.Serializable

@Serializable
data class FFProbeFormat(
    val filename: String,
    val duration: String
)
