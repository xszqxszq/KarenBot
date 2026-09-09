package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 水鱼 Rating 查询响应
 *
 * @property username 水鱼账号名
 * @property nickname 游戏昵称
 * @property charts 玩家标准与 DX 谱面的成绩分组
 */
@Serializable
data class DivingFishRatingResponse(
    val username: String,
    val rating: Int,
    @SerialName("additional_rating")
    val additionalRating: Int,
    val nickname: String,
    val plate: String ?= null,
    val charts: DivingFishCharts
)