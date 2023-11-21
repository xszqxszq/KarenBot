package xyz.xszq.nereides.payload.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageArk(
    @SerialName("template_id")
    val templateId: Int,
    val kv: List<MessageArkKv>
)
