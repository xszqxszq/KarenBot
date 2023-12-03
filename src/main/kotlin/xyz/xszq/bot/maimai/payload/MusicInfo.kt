package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MusicInfo(
    val id: String,
    val title: String,
    val type: String,
    var ds: MutableList<Double>,
    var level: MutableList<String>,
    val cids: List<Int>,
    val charts: List<ChartInfo>,
    @SerialName("basic_info")
    val basicInfo: MusicBasicInfo
) {
    fun getDXScoreMax(level: Int) = charts[level].notes.sum() * 3
    fun getDXStar(dxScore: Int, level: Int): Int {
        val max = getDXScoreMax(level)
        return when (100.0 * dxScore / max) {
            in 0.0 .. 84.9999 -> 0
            in 85.00..89.9999 -> 1
            in 90.00..92.9999 -> 2
            in 93.00..94.9999 -> 3
            in 95.00..96.9999 -> 4
            in 97.00..100.0000 -> 5
            else -> throw IllegalArgumentException()
        }
    }
}