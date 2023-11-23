package xyz.xszq.nereides.payload.post

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xszq.nereides.payload.message.Media
import xyz.xszq.nereides.payload.message.MessageArk
import xyz.xszq.nereides.payload.message.MessageMarkdownC2C

@Serializable
data class PostGroupMessage(
    val content: String,
    @SerialName("msg_type")
    val msgType: Int,
    val markdown: MessageMarkdownC2C? = null,
    val media: Media? = null,
    val ark: MessageArk? = null,
    @SerialName("event_id")
    val eventId: String? = null,
    @SerialName("msg_id")
    val msgId: String? = null,
    val msgSeq: Int? = null
)
