package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class LocalIconInfo(
    val id: Int,
    val filename: String,
    val name: String,
    val genre: String,
    val hint: String
)
