package xyz.xszq.nereides.payload.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xszq.nereides.payload.message.QQAttachment
import xyz.xszq.nereides.payload.user.QQUser

@Serializable
data class GroupAtMessageCreate(
    val author: QQUser,
    val content: String,
    @SerialName("group_id")
    val groupId: String,
    val id: String,
    val timestamp: String,
    val attachments: List<QQAttachment>? = null
)
