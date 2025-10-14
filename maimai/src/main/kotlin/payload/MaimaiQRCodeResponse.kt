package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class MaimaiQRCodeResponse(
    val userID: Long
)
