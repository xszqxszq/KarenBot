package xyz.xszq.bot.payload.markdown

import kotlinx.serialization.Serializable

/**
 * Markdown 模板消息的参数项
 */
@Serializable
data class MarkdownParam(
    val key: String,
    var values: List<String>
)