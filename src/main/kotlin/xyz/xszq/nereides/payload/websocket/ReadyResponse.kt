package xyz.xszq.nereides.payload.websocket

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xszq.nereides.payload.user.BotUser

@Serializable
data class ReadyResponse(
    val version: Int,
    @SerialName("session_id")
    val sessionId: String,
    val user: BotUser,
    val shard: List<Int>
)
