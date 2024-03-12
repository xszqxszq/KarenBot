package xyz.xszq.bot.rhythmgame.lxns.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SongDifficulty(
    val difficulty: LevelIndex,
    val level: String,
    @SerialName("level_value")
    val levelValue: Double,
    @SerialName("note_designer")
    val noteDesigner: String,
    val version: Int,
    val notes: Notes? = null
)
