package xyz.xszq.bot.chunithm.music

import kotlinx.serialization.Serializable

@Serializable
class MusicInfo(
    val id: Int,
    val name: String,
    val rights: String ?= null,
    val artist: String,
    val genre: MusicGenre,
    val bpm: Int,
    val version: GameVersion,
    val locked: Boolean,
    val disabled: Boolean,
    val map: String ?= null
) {
    var charts: List<ChartInfo> = listOf()
}