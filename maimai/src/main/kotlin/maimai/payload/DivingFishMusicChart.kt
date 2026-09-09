package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

/**
 * 水鱼曲目数据中一张谱面的信息
 *
 * @property notes 谱面各键型的音符数列表
 * @property charter 谱师名
 */
@Serializable
data class DivingFishMusicChart(
    val notes: List<Int>,
    val charter: String
)