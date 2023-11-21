package xyz.xszq.nereides.payload.post

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xszq.nereides.payload.message.*

@Serializable
data class PostChannelMessage(
    val content: String? = null,
    val embed: MessageEmbed? = null,
    val ark: MessageArk? = null,
    @SerialName("message_reference")
    val messageReference: MessageReference? = null,
    val image: String? = null,
    @SerialName("msg_id")
    val msgId: String? = null,
    @SerialName("event_id")
    val eventId: String? = null,
    val markdown: MessageMarkdown? = null
)
