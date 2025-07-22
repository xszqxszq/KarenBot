package xyz.xszq.bot.touhou

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val name: String,
    val games: List<Game>
)
