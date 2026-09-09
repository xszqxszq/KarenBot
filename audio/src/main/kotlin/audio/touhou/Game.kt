package xyz.xszq.bot.audio.touhou

import kotlinx.serialization.Serializable

/**
 * 东方官方作品
 */
@Serializable
data class Game(
    val id: String,
    val name: String,
    val tracks: List<Music>
)