package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
