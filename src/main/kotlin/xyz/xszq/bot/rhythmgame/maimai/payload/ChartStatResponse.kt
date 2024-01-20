package xyz.xszq.bot.rhythmgame.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChartStatResponse(
    val charts: HashMap<String, List<ChartStat>>,
    @SerialName("diff_data")
    val diffData: HashMap<String, ChartLevelStat>
)
