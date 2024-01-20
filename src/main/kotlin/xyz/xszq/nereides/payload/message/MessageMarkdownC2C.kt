package xyz.xszq.nereides.payload.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageMarkdownC2C(
    val content: String? = null,
    @SerialName("custom_template_id")
    val customTemplateId: String? = null,
    val params: List<MessageMarkdownKV>? = null
) {

    class Builder {
        private var desc = ""
        private var prompt = ""
        private val list = mutableListOf<MessageMarkdownKV>()
        fun append(key: String, block: () -> String) {
            list.add(MessageMarkdownKV(key, listOf(block())))
        }
        fun build(templateId: String): MessageMarkdownC2C {
            return MessageMarkdownC2C(customTemplateId = templateId, params = list)
        }
    }
}
