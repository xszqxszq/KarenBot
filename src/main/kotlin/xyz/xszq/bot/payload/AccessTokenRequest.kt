package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

/**
 * 获取应用访问令牌的请求
 */
@Serializable
data class AccessTokenRequest(
    val appId: String,
    val clientSecret: String,
)