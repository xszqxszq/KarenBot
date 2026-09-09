package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 水鱼接口中的单曲游玩成绩
 *
 * @property fc 全连状态码
 * @property fs 同步状态码
 * @property levelIndex 谱面难度序号
 * @property levelLabel 该难度的标签
 */
@Serializable
data class DivingFishRecord(
    val achievements: Double,
    val ds: Double,
    val dxScore: Int,
    val fc: String,
    val fs: String,
    val level: String,
    @SerialName("level_index")
    val levelIndex: Int,
    @SerialName("level_label")
    val levelLabel: String,
    val ra: Int,
    val rate: String,
    @SerialName("song_id")
    val songId: Int,
    val title: String,
    val type: String
)