package xyz.xszq.bot.payload.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 单聊相关事件
 *
 * @property user 用户的 OpenID
 * @property timestamp 事件时间戳
 */
@Serializable
data class C2CBotUpdate(
    @SerialName("openid")
    val user: String,
    val timestamp: Long
)