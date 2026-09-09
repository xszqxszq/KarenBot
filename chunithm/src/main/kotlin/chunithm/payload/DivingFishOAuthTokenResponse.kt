package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 水鱼 OAuth 令牌响应
 *
 * @property refreshToken 刷新令牌，仅在刷新授权方式下返回
 * @property idToken 身份令牌，仅在相应授权方式下返回
 * @property expiresIn 令牌有效期（秒）
 */
@Serializable
data class DivingFishOAuthTokenResponse(
    @SerialName("token_type")
    val tokenType: String,
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("expires_in")
    val expiresIn: Int,
    @SerialName("refresh_token")
    val refreshToken: String ?= null,
    val scope: String ?= null,
    @SerialName("id_token")
    val idToken: String ?= null
)