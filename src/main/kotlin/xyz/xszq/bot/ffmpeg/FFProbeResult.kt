package xyz.xszq.bot.ffmpeg

import kotlinx.serialization.Serializable

/**
 * FFProbe 的完整探测结果
 */
@Serializable
data class FFProbeResult(
    val streams: List<FFProbeStream>? = null,
    val format: FFProbeFormat? = null
)