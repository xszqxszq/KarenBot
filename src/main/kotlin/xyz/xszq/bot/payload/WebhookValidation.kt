package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Webhook 地址验证请求
 */
@Serializable
data class WebhookValidation(
    @SerialName("plain_token")
    val plainToken: String,
    @SerialName("event_ts")
    val eventTs: String
)