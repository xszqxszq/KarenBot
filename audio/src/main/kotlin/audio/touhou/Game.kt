package xyz.xszq.bot.audio.touhou

import kotlinx.serialization.Serializable

@Serializable
data class Game(
    val id: String,
    val name: String,
    val tracks: List<Music>
)