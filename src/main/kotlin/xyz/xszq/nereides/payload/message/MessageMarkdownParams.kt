package xyz.xszq.nereides.payload.message

import kotlinx.serialization.Serializable

@Serializable
data class MessageMarkdownParams(
    val key: String,
    val values: List<String>
)
