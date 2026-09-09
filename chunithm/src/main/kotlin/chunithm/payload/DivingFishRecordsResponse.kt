package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

/**
 * 水鱼成绩记录查询响应
 *
 * @property username 水鱼账号名
 * @property nickname 游戏昵称
 */
@Serializable
data class DivingFishRecordsResponse(
    val username: String,
    val nickname: String,
    val rating: Double,
    val records: DivingFishRecordsBests
)