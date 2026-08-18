package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DivingFishChartDiffData(
    val achievements: Double,
    @SerialName("dist")
    val distribution: List<Double>,
    @SerialName("fc_dist")
    val fullComboDistribution: List<Double>
)