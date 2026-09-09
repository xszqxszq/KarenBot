package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 落雪玩家 Best 50 成绩响应
 *
 * @property bests 旧曲最佳成绩列表
 * @property newBests 新曲最佳成绩列表
 */
@Serializable
data class LXNSRatingResponse(
    val bests: List<LXNSScore>,
    val selections: List<LXNSScore> = listOf(),
    @SerialName("new_bests")
    val newBests: List<LXNSScore>
)