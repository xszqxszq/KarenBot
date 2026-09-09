package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

/**
 * 水鱼玩家的最佳成绩列表
 *
 * @property best 每首歌曲一条的最佳成绩
 */
@Serializable
data class DivingFishRecordsBests(
    val best: List<DivingFishRecord>
)