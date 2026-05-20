package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class FaceExt(
    val text: String,
    val num: Int = 0
)
