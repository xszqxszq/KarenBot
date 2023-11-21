package xyz.xszq.nereides.payload.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageMarkdown(
    val content: String? = null,
    @SerialName("template_id")
    val templateId: String? = null,
    val params: List<MessageMarkdownParams>? = null
)
