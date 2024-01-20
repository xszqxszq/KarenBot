package xyz.xszq.bot.rhythmgame.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MusicBasicInfo(
    val title: String,
    val artist: String,
    val genre: String,
    val bpm: Int,
    @SerialName("release_date")
    val releaseDate: String,
    val from: String,
    @SerialName("is_new")
    val isNew: Boolean
)