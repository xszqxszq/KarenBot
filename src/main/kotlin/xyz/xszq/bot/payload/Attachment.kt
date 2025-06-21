package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Attachment(
    val url: String,
    val filename: String,
    val width: Int,
    val height: Int,
    val size: Int,
    @SerialName("content_type")
    val contentType: String,
    val content: String ?= null,
)
