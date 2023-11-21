package xyz.xszq.nereides.payload.websocket

import kotlinx.serialization.Serializable
import xyz.xszq.nereides.JsonAsStringSerializer

@Serializable
data class PayloadRecv(
    val op: Int,
    @Serializable(with = JsonAsStringSerializer::class)
    val d: String? = null,
    val s: Int? = null,
    val t: String? = null,
    val id: String ?= null
)
