package xyz.xszq.bot.ffmpeg

import kotlinx.serialization.Serializable

/**
 * FFProbe 输出的容器格式信息
 */
@Serializable
data class FFProbeFormat(
    val filename: String,
    val duration: String? = null
)