package xyz.xszq.bot.payload.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 群成员变更事件
 *
 * @property group 相关群的 OpenID
 * @property member 发生变更的成员 OpenID
 * @property timestamp 事件时间戳
 */
@Serializable
data class GroupMemberUpdate(
    @SerialName("group_openid")
    val group: String,
    @SerialName("member_openid")
    val member: String,
    val timestamp: Long,
)