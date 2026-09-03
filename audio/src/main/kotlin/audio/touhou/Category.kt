package xyz.xszq.bot.audio.touhou

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val name: String,
    val games: List<Game>
)