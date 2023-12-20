package xyz.xszq.nereides.payload.message

import kotlinx.serialization.Serializable

@Serializable
data class MessageMarkdownKV(
    val key: String,
    val values: List<String>
)
