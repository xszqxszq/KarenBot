package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.payload.markdown.MarkdownData

/**
 * 向用户/群发送消息的请求
 */
@Serializable
data class MessagePayload(
    var content: String,
    @SerialName("msg_type")
    val msgType: Int,
    val markdown: MarkdownData ?= null,
    val keyboard: Keyboard ?= null,
    val eventId: String ?= null,
    val msgId: String ?= null,
    var msgSeq: Int = 1,
    val media: FileResponse? = null,
)