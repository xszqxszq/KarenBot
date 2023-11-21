package xyz.xszq.nereides.payload.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QQAttachment(
    @SerialName("content_type")
    val contentType: String,
    val filename: String,
    val height: String ?= null,
    val width: String ?= null,
    val size: String,
    val url: String
) {
    fun isImage() = contentType.split("/").first() == "image"
}
