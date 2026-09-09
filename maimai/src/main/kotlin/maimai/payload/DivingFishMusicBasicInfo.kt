package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 水鱼曲目数据中的曲目基本信息
 *
 * @property from 曲目的出处信息
 * @property isNew 是否为本版本新增的曲目
 */
@Serializable
data class DivingFishMusicBasicInfo(
    val title: String,
    val artist: String,
    val genre: String,
    val bpm: Int,
    @SerialName("release_date")
    val releaseDate: String,
    val from: String,
    @SerialName("is_new")
    val isNew: Boolean
)