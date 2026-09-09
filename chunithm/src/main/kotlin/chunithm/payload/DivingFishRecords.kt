package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

/**
 * 水鱼 Rating 响应中的成绩分组
 *
 * @property b30 旧曲最佳成绩列表
 * @property n20 新曲最佳成绩列表
 */
@Serializable
data class DivingFishRecords(
    val b30: List<DivingFishRecord> = listOf(),
    val n20: List<DivingFishRecord> = listOf(),
)