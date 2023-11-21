package xyz.xszq.nereides.payload.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageMarkdownC2C(
    val content: String? = null,
    @SerialName("custom_template_id")
    val customTemplateId: String? = null,
    val params: List<MessageArkKv>? = null
)
