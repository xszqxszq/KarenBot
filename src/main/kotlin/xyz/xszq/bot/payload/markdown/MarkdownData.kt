package xyz.xszq.bot.payload.markdown

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xszq.bot.message.Markdown

@Serializable
data class MarkdownData(
    val content: String ?= null,
    @SerialName("custom_template_id")
    val customTemplateId: String ?= null,
    val params: List<MarkdownParam> ?= null
) {
    fun toMessage(keyboard: Keyboard ?= null) = Markdown(this, keyboard)
    fun text() = content ?: params ?.joinToString(", ") {
        "${it.key}=${it.values.first().replace("\r", "\n")}"
    }
    class MarkdownBuilder(
        val id: String
    ) {
        val data = mutableListOf<MarkdownParam>()
        fun build() = MarkdownData(
            customTemplateId = id,
            params = data)
        operator fun String.invoke(block: () -> String) {
            data.add(MarkdownParam(this, listOf(block.invoke())))
        }
    }
    companion object {
        fun create(
            id: String,
            builder: MarkdownBuilder.() -> Unit
        ) = MarkdownBuilder(id).apply(builder).build()
    }
}