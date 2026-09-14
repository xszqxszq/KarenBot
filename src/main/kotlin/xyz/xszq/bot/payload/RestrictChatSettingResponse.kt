package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 群禁言状态响应
 */
@Serializable
data class RestrictChatSettingResponse(
    @SerialName("global_rule")
    val globalRule: GlobalMuteRule ?= null,
    val members: List<MemberMuteState> = listOf()
)