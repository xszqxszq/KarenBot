package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

/**
 * 水鱼 Rating 查询响应
 *
 * @property username 水鱼账号名
 * @property nickname 游戏昵称
 * @property records 按新旧曲分组的最佳成绩
 */
@Serializable
data class DivingFishRatingResponse(
    val username: String,
    val nickname: String,
    val rating: Double,
    val records: DivingFishRecords
)