package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 水鱼接口中的单曲游玩成绩
 *
 * @property fc 连击状态码
 * @property levelIndex 谱面难度序号
 * @property levelLabel 该难度的标签
 * @property mid 本地曲库的歌曲 ID
 */
@Serializable
data class DivingFishRecord(
    val cid: Int,
    val ds: Double,
    val fc: String = "",
    val level: String,
    @SerialName("level_index")
    val levelIndex: Int,
    @SerialName("level_label")
    val levelLabel: String,
    val mid: Int,
    val ra: Double,
    val score: Int,
    val title: String
)