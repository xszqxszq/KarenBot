package xyz.xszq.bot.audio.touhou

import kotlinx.serialization.Serializable

/**
 * 东方原曲类别
 */
@Serializable
data class Category(
    val name: String,
    val games: List<Game>
)