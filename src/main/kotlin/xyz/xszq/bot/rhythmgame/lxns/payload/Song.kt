package xyz.xszq.bot.rhythmgame.lxns.payload

import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val genre: String,
    val bpm: Int,
    val version: Int,
    val difficulties: List<SongDifficulty>
)
