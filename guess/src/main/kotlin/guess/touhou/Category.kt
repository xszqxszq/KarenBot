package xyz.xszq.bot.guess.touhou

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val name: String,
    val games: List<Game>
)
