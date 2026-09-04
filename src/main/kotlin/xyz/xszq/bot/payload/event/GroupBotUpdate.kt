package xyz.xszq.bot.payload.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 群聊相关事件
 *
 * @property group 相关群的 OpenID
 * @property operator 操作成员的 OpenID
 * @property timestamp 事件时间戳
 */
@Serializable
data class GroupBotUpdate(
    @SerialName("group_openid")
    val group: String,
    @SerialName("op_member_openid")
    val operator: String,
    val timestamp: Long,
)