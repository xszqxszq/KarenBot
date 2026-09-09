package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 水鱼曲库数据中的一首曲目
 *
 * @property ds 各难度谱面的定数列表
 * @property level 各难度谱面的等级列表
 * @property cidList 各谱面对应的谱面 ID 列表
 * @property charts 按难度顺序排列的谱面列表
 */
@Serializable
data class DivingFishMusicInfo(
    val id: String,
    val title: String,
    val type: String,
    val ds: List<Double>,
    val level: List<String>,
    @SerialName("cids")
    val cidList: List<String>,
    val charts: List<DivingFishMusicChart>,
    @SerialName("basic_info")
    val basicInfo: DivingFishMusicBasicInfo
)