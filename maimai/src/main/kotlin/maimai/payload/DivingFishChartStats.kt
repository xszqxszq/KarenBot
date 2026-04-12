package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DivingFishChartStats(
    @SerialName("cnt")
    val count: Double ?= null,
    @SerialName("diff")
    val level: String ?= null,
    @SerialName("fit_diff")
    val fitLevelValue: Double ?= null,
    @SerialName("avg")
    val average: Double ?= null,
    @SerialName("avg_dx")
    val averageDXScore: Double ?= null,
    @SerialName("std_dev")
    val standardDeviation: Double ?= null,
    @SerialName("dist")
    val distribution: List<Double> ?= null,
    @SerialName("fc_dist")
    val fullComboDistribution: List<Double> ?= null
)
