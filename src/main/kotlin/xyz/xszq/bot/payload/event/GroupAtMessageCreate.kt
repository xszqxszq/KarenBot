package xyz.xszq.bot.payload.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xszq.bot.payload.Attachment
import xyz.xszq.bot.payload.Member

@Serializable
data class GroupAtMessageCreate(
    val id: String,
    val author: Member,
    val content: String,
    val timestamp: String,
    @SerialName("group_openid")
    val group: String,
    val attachments: List<Attachment> = listOf()
)
