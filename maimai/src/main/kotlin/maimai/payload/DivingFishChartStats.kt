package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 水鱼 chart_stats 接口中一张谱面的统计数据
 *
 * 列表按难度顺序排列
 *
 * @property level 谱面等级字符串
 * @property distribution 达成率区间的人数比例分布
 * @property fullComboDistribution 全连状态的人数比例分布
 */
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