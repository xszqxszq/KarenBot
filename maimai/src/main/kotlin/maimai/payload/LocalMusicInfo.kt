package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

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