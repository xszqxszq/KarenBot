package xyz.xszq.bot.chunithm.music

import kotlinx.serialization.Serializable
import xyz.xszq.bot.newLine

@Serializable
class ChartInfo(
    val music: MusicInfo,
    val difficulty: MusicDifficulty,
    val level: String,
    val levelValue: Double,
    val notes: Notes,
    val notesDesigner: String,
    val kanji: String ?= null,
    val star: Int ?= null
) {
    var origin: MusicInfo ?= null

    fun infoText() = buildString {
        appendLine("${difficulty.brief}${music.id}. ${music.name}")
        appendLine("等级: $level (${levelValue})")
        appendLine("TAP: ${notes.tap}")
        appendLine("HOLD: ${notes.hold}")
        appendLine("SLIDE: ${notes.slide}")
        appendLine("AIR: ${notes.air}")
        appendLine("FLICK: ${notes.flick}")
        appendLine("总物量: ${notes.total}")
        appendLine("谱师: ${notesDesigner.ifBlank { "-" }}")
        kanji ?.let { value ->
            appendLine("属性: $value")
        }
        star ?.let { value ->
            appendLine("星级: $value")
        }
        origin ?.let { value ->
            appendLine("原曲: ${value.id}. ${value.name}")
        }
    }.trim().newLine()
}