package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

@Serializable
data class LXNSCollection(
    val id: Int,
    val name: String,
    val color: String ?= null,
    val level: Int ?= null
)