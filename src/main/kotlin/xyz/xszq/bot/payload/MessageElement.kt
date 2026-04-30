package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xszq.bot.message.MessageChain

@Serializable
data class MessageElement(
    @SerialName("msg_idx")
    val id: String,
    val author: MessageAuthor,
    @SerialName("message_type")
    val messageType: Int,
    val content: String,
    val attachments: List<Attachment> = listOf(),
) {
    fun toMessageChain() = MessageChain()
}