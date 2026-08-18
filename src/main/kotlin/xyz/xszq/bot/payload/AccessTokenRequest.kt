package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class AccessTokenRequest(
    val appId: String,
    val clientSecret: String,
)