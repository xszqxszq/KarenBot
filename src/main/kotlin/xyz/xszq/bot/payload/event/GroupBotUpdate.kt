package xyz.xszq.bot.payload.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupBotUpdate(
    @SerialName("group_openid")
    val group: String,
    @SerialName("op_member_openid")
    val operator: String,
    val timestamp: Long,
)