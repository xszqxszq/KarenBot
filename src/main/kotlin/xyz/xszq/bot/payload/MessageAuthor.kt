package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class MessageAuthor(
    val username: String,
    val bot: Boolean,
)