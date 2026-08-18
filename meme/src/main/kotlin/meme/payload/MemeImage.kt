package xyz.xszq.bot.meme.payload

import kotlinx.serialization.Serializable

@Serializable
data class MemeImage(
    val name: String,
    val id: String
)
