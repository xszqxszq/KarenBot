package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 水鱼 chart_stats 接口的谱面统计数据
 *
 * 插件用其结果补充本地谱面的拟合定数
 *
 * @property charts 按曲目 ID 分组的谱面统计，列表按难度顺序排列
 * @property diffData 按等级聚合的统计
 */
@Serializable
data class DivingFishStats(
    val charts: Map<String, List<DivingFishChartStats>>,
    @SerialName("diff_data")
    val diffData: Map<String, DivingFishChartDiffData>,
)