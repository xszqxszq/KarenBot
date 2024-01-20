package xyz.xszq.bot.rhythmgame.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChartStat(
    val cnt: Double ?= null,
    val diff: String ?= null,
    @SerialName("fit_diff")
    val fitDiff: Double ?= null,
    val avg: Double ?= null,
    @SerialName("avg_dx")
    val avgDx: Double ?= null,
    @SerialName("std_dev")
    val stdDev: Double ?= null,
    val dist: List<Double> ?= null,
    @SerialName("fc_dist")
    val fcDist: List<Double> ?= null
)