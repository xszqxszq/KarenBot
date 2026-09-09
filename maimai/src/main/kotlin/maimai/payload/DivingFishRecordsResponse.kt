package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 水鱼成绩记录查询响应
 *
 * 由 player/records 接口返回并可按曲目 ID 过滤
 *
 * @property username 水鱼账号名
 * @property nickname 游戏昵称
 */
@Suppress("unused")
@Serializable
data class DivingFishRecordsResponse(
    val username: String,
    val rating: Int,
    @SerialName("additional_rating")
    val additionalRating: Int,
    val nickname: String,
    val plate: String ?= null,
    val records: List<DivingFishRecord>
)