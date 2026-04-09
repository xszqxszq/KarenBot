package xyz.xszq.bot.payload.markdown

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xszq.bot.message.Markdown
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Serializable
data class MarkdownData(
    val content: String ?= null,
    @SerialName("custom_template_id")
    val customTemplateId: String ?= null,
    val params: List<MarkdownParam> ?= null
) {
    fun toMessage(keyboard: Keyboard ?= null) = Markdown(this, keyboard)
    fun text() = content?.formatContentForDisplay() ?: params ?.joinToString(", ") {
        "${it.key}=${it.values.first().replace("\r", "\n")}"
    }

    private fun String.formatContentForDisplay(): String {
        val normalized = replace("\r", "\n")
        return normalized
            .replace(QQ_BOT_CMD_TAG_REGEX) { match ->
                val attrs = ATTRIBUTE_REGEX.findAll(match.value).associate { result ->
                    result.groupValues[1] to decodeUrlComponent(result.groupValues[2])
                }
                val show = attrs["show"] ?: attrs["text"] ?: match.value
                val text = attrs["text"] ?: attrs["url"] ?: show
                "[$show]($text)"
            }
            .replace(MARKDOWN_IMAGE_REGEX, "![img](...)")
            .formatMarkdownTables()
    }

    private fun String.formatMarkdownTables(): String {
        val lines = lines()
        val output = mutableListOf<String>()
        var index = 0

        while (index < lines.size) {
            val header = lines[index]
            val separator = lines.getOrNull(index + 1)
            if (header.isMarkdownTableRow() && separator?.isMarkdownTableSeparator() == true) {
                output += header
                output += ""
                index += 2
                while (index < lines.size && lines[index].isMarkdownTableRow()) {
                    output += lines[index]
                    index += 1
                }
                continue
            }
            output += header
            index += 1
        }

        return output.joinToString("\n")
    }

    private fun String.isMarkdownTableRow(): Boolean {
        val trimmed = trim()
        return trimmed.startsWith("|") && trimmed.endsWith("|")
    }

    private fun String.isMarkdownTableSeparator(): Boolean {
        val trimmed = trim()
        return trimmed.matches(Regex("^\\|(?:\\s*:?-{3,}:?\\s*\\|)+$"))
    }

    private fun decodeUrlComponent(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8)

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
        private val QQ_BOT_CMD_TAG_REGEX = Regex("""<qqbot-cmd-[^\s>/]+\s+[^>]*?/>""")
        private val ATTRIBUTE_REGEX = Regex("(\\w+)=\"([^\"]*)\"")
        private val MARKDOWN_IMAGE_REGEX = Regex("""!\[[^\]]*]\([^)]*\)""")

        fun create(
            id: String,
            builder: MarkdownBuilder.() -> Unit
        ) = MarkdownBuilder(id).apply(builder).build()
    }
}
