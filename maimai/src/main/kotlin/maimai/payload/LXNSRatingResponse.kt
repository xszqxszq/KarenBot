package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

/**
 * 落雪玩家 Best 50 成绩响应
 *
 * @property standard 标准谱面 Best 35 列表
 * @property dx DX 谱面 Best 15 列表
 */
@Serializable
data class LXNSRatingResponse(
    val standard: List<LXNSScore>,
    val dx: List<LXNSScore>
)