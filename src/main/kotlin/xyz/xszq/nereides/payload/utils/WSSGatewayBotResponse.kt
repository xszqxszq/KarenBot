package xyz.xszq.nereides.payload.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WSSGatewayBotResponse(
    val url: String,
    val shards: Int,
    @SerialName("session_start_limit")
    val sessionStartLimit: SessionStartLimit
)
