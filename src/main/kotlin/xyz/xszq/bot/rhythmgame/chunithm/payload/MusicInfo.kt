package xyz.xszq.bot.rhythmgame.chunithm.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MusicInfo(
    val id: String,
    val title: String,
    var ds: MutableList<Double>,
    var level: MutableList<String>,
    val cids: List<Int>,
    val charts: List<ChartInfo>,
    @SerialName("basic_info")
    val basicInfo: MusicBasicInfo
)
