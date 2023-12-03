package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
open class PlayScore(
    val achievements: Double,
    val ds: Double,
    val dxScore: Int,
    val fc: String,
    val fs: String,
    val level: String,
    @SerialName("level_index")
    val levelIndex: Int,
    @SerialName("level_label")
    val levelLabel: String,
    var ra: Int,
    val rate: String,
    @SerialName("song_id")
    val songId: Int,
    val title: String,
    val type: String
) {
    companion object {
        fun fillEmpty(list: List<PlayScore>, target: Int): List<PlayScore> {
            val result = list.toMutableList()
            for (i in 1..(target-list.size))
                result.add(PlayScore(0.0, .0, 0, "", "", "",
                    0, "", 0, "", -1, "", ""))
            return result
        }
    }
}