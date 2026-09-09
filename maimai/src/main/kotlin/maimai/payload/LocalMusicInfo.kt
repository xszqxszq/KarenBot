package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

/**
 * 本地歌曲信息
 *
 * @property type 曲目所属的标准谱面或 DX 谱面类型
 * @property charts 按难度序号顺序列出的全部谱面
 */
@Serializable
data class LocalMusicInfo(
    val id: Int,
    val name: String,
    val type: String,
    val rights: String,
    val artist: String,
    val genre: String,
    val bpm: Int,
    val version: String,
    val charts: List<LocalChartInfo>
)