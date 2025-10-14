package xyz.xszq.bot.music

import kotlinx.serialization.Serializable

@Serializable
data class GameVersion(
    val id: Int,
    val name: String,
    val version: Int
)
