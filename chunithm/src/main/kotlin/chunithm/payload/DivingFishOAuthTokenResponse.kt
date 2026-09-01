package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
