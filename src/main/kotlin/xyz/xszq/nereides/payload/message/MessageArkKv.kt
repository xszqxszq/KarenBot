package xyz.xszq.nereides.payload.message

import kotlinx.serialization.Serializable

@Serializable
data class MessageArkKv(
    val key: String,
    val value: String? = null,
    val obj: List<MessageArkObj>? = null
)
