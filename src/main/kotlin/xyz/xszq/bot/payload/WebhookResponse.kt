package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WebhookResponse(
    @SerialName("plain_token")
    val plainToken: String,
    val signature: String
)
