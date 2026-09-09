package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

/**
 * 水鱼 Rating 响应中的成绩分组
 *
 * @property sd 标准谱面的成绩列表
 * @property dx DX 谱面的成绩列表
 */
@Serializable
data class DivingFishCharts(
    val sd: List<DivingFishRecord>,
    val dx: List<DivingFishRecord>
)