package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable
import xyz.xszq.bot.JsonAsStringSerializer

@Serializable
data class Payload(
    val op: Int,
    @Serializable(with = JsonAsStringSerializer::class)
    val d: String ?= null,
    val s: Int ?= null,
    val t: String? = null,
    val id: String? = null,
)
