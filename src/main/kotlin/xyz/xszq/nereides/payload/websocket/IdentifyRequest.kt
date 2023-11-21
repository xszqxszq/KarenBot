package xyz.xszq.nereides.payload.websocket

import kotlinx.serialization.Serializable

@Serializable
data class IdentifyRequest(
    val token: String,
    val intents: Int,
    val shard: List<Int>
)
