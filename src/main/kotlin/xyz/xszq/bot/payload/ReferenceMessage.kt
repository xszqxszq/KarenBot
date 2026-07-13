package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xszq.bot.message.MessageChain
import xyz.xszq.bot.message.RemoteImage
import xyz.xszq.bot.message.RemoteVoice

@Serializable
data class ReferenceMessage(
    @SerialName("msg_idx")
    val id: String ?= null,
    val author: MessageAuthor ?= null,
    @SerialName("message_type")
    val messageType: Int ?= null,
    val content: String,
    val attachments: List<Attachment> = listOf(),
) {
    fun toMessageChain(): MessageChain {
        val chain = MessageChain(content)
        attachments.forEach { attachment ->
            when {
                "image" in attachment.contentType -> {
                    chain += RemoteImage(
                        url = attachment.url,
                        filename = attachment.filename,
                        contentType = attachment.contentType,
                    )
                }
                "voice" in attachment.contentType -> {
                    chain += RemoteVoice(
                        url = attachment.url,
                        filename = attachment.filename,
                        contentType = attachment.contentType,
                        wavUrl = attachment.voiceWavUrl ?: "",
                    )
                }
            }
        }
        return chain
    }
}