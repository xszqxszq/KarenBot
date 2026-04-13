package xyz.xszq.bot.chunithm.music

import kotlinx.serialization.Serializable
import xyz.xszq.bot.newLine

@Serializable
class MusicInfo(
    val id: Int,
    val name: String,
    val rights: String ?= null,
    val artist: String,
    val genre: MusicGenre,
    val bpm: Int,
    val version: GameVersion,
    val isNew: Boolean = false,
    val locked: Boolean = false,
    val disabled: Boolean = false,
    val map: String ?= null
) {
    var charts: List<ChartInfo> = listOf()

    fun infoText() = buildString {
        appendLine("${id}. $name")
        appendLine("曲师: $artist")
        appendLine("分类: ${genre.genreName}")
        appendLine("版本: ${version.name}${if (isNew) " (计入n20)" else ""}")
        appendLine("BPM: $bpm")
        appendLine("定数: ${charts.joinToString("/") { chart -> chart.levelValue.toString() }}")
        appendLine("谱师: ${charts.joinToString("/") { chart -> chart.notesDesigner.ifBlank { "-" } }}")
    }.trim().newLine()
}