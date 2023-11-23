package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChartLevelStat(
    val achievements: Double,
    val dist: List<Double>,
    @SerialName("fc_dist")
    val fc_dist: List<Double>
)
