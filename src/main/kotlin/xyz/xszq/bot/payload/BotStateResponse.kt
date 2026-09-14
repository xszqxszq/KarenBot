package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 机器人群内状态
 */
@Serializable
data class BotStateResponse(
    @SerialName("member_openid")
    val member: String = "",
    @SerialName("joined_at")
    val joinedAt: String = "",
    @SerialName("allow_proactive_msg")
    val allowPush: Boolean = false,
    @SerialName("recv_msg_setting")
    val recvMsgSetting: String = "",
    @SerialName("member_role")
    val role: String = "member"
)