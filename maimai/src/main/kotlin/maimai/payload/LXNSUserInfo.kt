package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 落雪 OAuth 用户信息
 *
 * 由 userinfo 接口返回
 *
 * @property sub 用户标识
 * @property preferredUsername 用户优先使用的用户名
 */
@Serializable
data class LXNSUserInfo(
    val sub: String ?= null,
    val name: String ?= null,
    @SerialName("preferred_username")
    val preferredUsername: String ?= null
)