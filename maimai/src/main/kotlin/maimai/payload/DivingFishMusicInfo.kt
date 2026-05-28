package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DivingFishMusicInfo(
    val id: String,
    val title: String,
    val type: String,
    val ds: List<Double>,
    val level: List<String>,
    val cids: List<String>,
    val charts: List<DivingFishMusicChart>,
    @SerialName("basic_info")
    val basicInfo: DivingFishMusicBasicInfo
)

@Serializable
data class DivingFishMusicChart(
    val notes: List<Int>,
    val charter: String
)

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

@Serializable
data class DivingFishMusicCache(
    val etag: String = "",
    val musics: List<DivingFishMusicInfo> = emptyList()
)
