package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 处于禁言中的群成员
 */
@Serializable
data class MemberMuteState(
    @SerialName("member_openid")
    val member: String = "",
    @SerialName("mute_expire_at")
    val muteExpireAt: String = "",
    val username: String = "",
    @SerialName("union_openid")
    val unionOpenId: String = ""
)