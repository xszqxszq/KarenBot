package xyz.xszq.nereides.payload.websocket

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HelloResponse(
    @SerialName("heartbeat_interval")
    val heartbeatInterval: Long
)
