package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 落雪 OAuth 令牌请求
 *
 * 授权码换取需要授权码与回调地址，刷新令牌换取需要刷新令牌
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