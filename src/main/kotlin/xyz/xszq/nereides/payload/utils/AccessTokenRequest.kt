package xyz.xszq.nereides.payload.utils

import kotlinx.serialization.Serializable

@Serializable
data class AccessTokenRequest(
    val appId: String,
    val clientSecret: String
)
