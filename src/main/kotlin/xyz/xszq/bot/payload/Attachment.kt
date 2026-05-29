package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Attachment(
    val url: String,
    val filename: String,
    val width: Int ?= null,
    val height: Int ?= null,
    val size: Int,
    @SerialName("content_type")
    val contentType: String,
    val content: String ?= null,
    @SerialName("voice_wav_url")
    val voiceWavUrl: String? = null,
    @SerialName("asr_refer_text")
    val asrReferText: String? = null,
)