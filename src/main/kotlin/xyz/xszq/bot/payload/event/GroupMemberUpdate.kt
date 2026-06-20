package xyz.xszq.bot.payload.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupMemberUpdate(
    @SerialName("group_openid")
    val group: String,
    @SerialName("member_openid")
    val member: String,
    val timestamp: Long,
)