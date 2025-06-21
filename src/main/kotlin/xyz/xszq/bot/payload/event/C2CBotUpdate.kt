package xyz.xszq.bot.payload.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class C2CBotUpdate(
    @SerialName("openid")
    val user: String,
    val timestamp: Long
)