package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DivingFishStats(
    val charts: Map<String, List<DivingFishChartStats>>,
    @SerialName("diff_data")
    val diffData: Map<String, DivingFishChartDiffData>,
)
