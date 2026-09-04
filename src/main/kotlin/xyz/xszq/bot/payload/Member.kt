package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 群会话中的成员信息
 */
@Serializable
data class Member(
    @SerialName("member_openid")
    val id: String = "",
    val username: String = "",
    val bot: Boolean = false,
    val scope: String = "single",
    @SerialName("is_you")
    val isSelf: Boolean = false,
    @SerialName("member_role")
    val role: String = "member"
)