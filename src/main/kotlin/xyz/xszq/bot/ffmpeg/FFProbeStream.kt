package xyz.xszq.bot.ffmpeg

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FFProbeStream(
    val index: Int,
    @SerialName("codec_name")
    val codecName: String,
    @SerialName("codec_long_name")
    val codecLongName: String,
    @SerialName("codec_type")
    val codecType: String,
    val duration: String,
)
