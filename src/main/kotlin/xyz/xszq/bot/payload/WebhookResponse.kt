package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Webhook 地址验证响应
 */
@Serializable
data class WebhookResponse(
    @SerialName("plain_token")
    val plainToken: String,
    val signature: String
)