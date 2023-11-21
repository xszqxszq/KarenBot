package xyz.xszq.nereides.payload.user

import kotlinx.serialization.Serializable

@Serializable
data class BotUser(
    val id: String,
    val username: String,
    val bot: Boolean
)
