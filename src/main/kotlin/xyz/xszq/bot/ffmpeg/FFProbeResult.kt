package xyz.xszq.bot.ffmpeg

import kotlinx.serialization.Serializable

@Serializable
data class FFProbeResult(
    val streams: List<FFProbeStream>? = null,
    val format: FFProbeFormat? = null
)
