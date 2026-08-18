package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

@Serializable
data class LXNSVersion(
    val id: Int,
    val title: String,
    val version: Int
)