package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 落雪 OAuth 令牌请求
 *
 * @property grantType 授权方式
 * @property code 授权码
 * @property redirectUri 回调地址
 * @property refreshToken 刷新令牌
 */
@Serializable
data class LXNSOAToken(
    @SerialName("client_id")
    val clientId: String,
    @SerialName("client_secret")
    val clientSecret: String,
    @SerialName("grant_type")
    val grantType: String,
    val code: String ?= null,
    @SerialName("redirect_uri")
    val redirectUri: String ?= null,
    @SerialName("refresh_token")
    val refreshToken: String ?= null
)