package xyz.xszq.otomadbot.api

import kotlinx.serialization.Serializable

@Serializable
data class CookiesResponse(
    val cookies: String,
    val bkn: String
)
