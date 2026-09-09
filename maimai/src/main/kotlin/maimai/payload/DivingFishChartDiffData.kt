package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 水鱼 chart_stats 接口按等级聚合的统计
 *
 * @property achievements 该等级的平均达成率
 * @property distribution 达成率区间的人数比例分布
 * @property fullComboDistribution 全连状态的人数比例分布
 */
@Serializable
data class DivingFishChartDiffData(
    val achievements: Double,
    @SerialName("dist")
    val distribution: List<Double>,
    @SerialName("fc_dist")
    val fullComboDistribution: List<Double>
)