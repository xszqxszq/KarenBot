package xyz.xszq.nereides.payload.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupDelBot(
    val timestamp: String,
    @SerialName("group_openid")
    val groupId: String,
    @SerialName("op_member_openid")
    val operator: String
)