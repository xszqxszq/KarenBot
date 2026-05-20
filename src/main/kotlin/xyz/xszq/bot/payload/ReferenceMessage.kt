package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xszq.bot.message.MessageChain
import xyz.xszq.bot.message.RemoteImage
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
data class ReferenceMessage(
    @SerialName("msg_idx")
    val id: String,
    val author: MessageAuthor,
    @SerialName("message_type")
    val messageType: Int,
    val content: String,
    val attachments: List<Attachment> = listOf(),
) {
    @OptIn(ExperimentalEncodingApi::class)
    fun toMessageChain(): MessageChain {
        val chain = MessageChain(content)
        attachments.forEach { attachment ->
            if ("image" in attachment.contentType) {
                chain += RemoteImage(
                    url = attachment.url,
                    filename = attachment.filename,
                    contentType = attachment.contentType,
                )
            }
        }
        return chain
    }
}