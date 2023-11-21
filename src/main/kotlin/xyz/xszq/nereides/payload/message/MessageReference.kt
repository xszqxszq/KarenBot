package xyz.xszq.nereides.payload.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageReference(
    @SerialName("message_id")
    val messageId: String,
    @SerialName("ignore_get_message_error")
    val ignoreGetMessageError: Boolean
)
