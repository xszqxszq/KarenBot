package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 水鱼接口中的单曲游玩成绩简版记录
 *
 * 由官方查分网页解析生成，并作为导入更新记录的请求
 */
@Serializable
data class DivingFishRecordSimple(
    val title: String,
    val achievements: Double,
    val dxScore: Int,
    val fc: String,
    val fs: String,
    @SerialName("level_index")
    val levelIndex: Int,
    val type: String
)