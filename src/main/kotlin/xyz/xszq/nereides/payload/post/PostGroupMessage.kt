package xyz.xszq.nereides.payload.post

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xszq.nereides.payload.message.MessageArk

@Serializable
data class PostGroupMessage(
    val content: String,
    @SerialName("msg_type")
    val msgType: Int,
    @SerialName("msg_id")
    val msgId: String,
    val messageArk: MessageArk? = null
)
