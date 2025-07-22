package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class MemeImage(
    val name: String,
    val id: String
)
