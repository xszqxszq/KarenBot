package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DivingFishMusicInfo(
    val id: String,
    val title: String,
    val type: String,
    val ds: List<Double>,
    val level: List<String>,
    val charts: List<DivingFishChartInfo>,
    @SerialName("basic_info")
    val basicInfo: DivingFishBasicInfo
)
