package xyz.xszq.bot.rhythmgame.chunithm.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayScore(
    val cid: Int,
    val ds: Double,
    val fc: String,
    val level: String,
    @SerialName("level_index")
    val levelIndex: Int,
    @SerialName("level_label")
    val levelLabel: String,
    val mid: Int,
    val ra: Double,
    val score: Int,
    val title: String
)
