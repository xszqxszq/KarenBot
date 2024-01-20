package xyz.xszq.bot.rhythmgame.chunithm.payload

import kotlinx.serialization.Serializable

@Serializable
data class MusicBasicInfo(
    val title: String,
    val artist: String,
    val genre: String,
    val bpm: Int,
    val from: String
)
