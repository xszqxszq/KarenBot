package xyz.xszq.bot.payload.markdown

import kotlinx.serialization.Serializable

@Serializable
data class MarkdownParam(
    val key: String,
    val values: List<String>
)