package xyz.xszq.nereides.payload.websocket

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class PayloadSend<T>(
    val op: Int,
    @Contextual
    val d: T
)