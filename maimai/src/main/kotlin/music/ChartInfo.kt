package xyz.xszq.bot.music

import xyz.xszq.bot.component.MarkdownTemplates
import xyz.xszq.bot.message.Image
import xyz.xszq.bot.newLine
import xyz.xszq.bot.plus

class ChartInfo(
    val music: MusicInfo,
    val difficulty: MusicDifficulty,
    val level: String,
    val levelValue: Double,
    val notes: Notes,
    val notesDesigner: String
) {
    val maxDeluxeScore: Int
        get() = notes.maxDeluxeScore

    var fitLevelValue: Double = 0.0

    suspend fun infoText() = Image(music.cover()) + buildString {
        appendLine("${difficulty.names.last()}${music.id}. ${music.name}")
        appendLine("等级: $level (${levelValue})")
        appendLine("TAP: ${notes.tap}")
        appendLine("HOLD: ${notes.hold}")
        appendLine("SLIDE: ${notes.slide}")
        appendLine("BREAK: ${notes.`break`}")
        if (notes.touch != 0)
            appendLine("TOUCH: ${notes.touch}")
        appendLine("总物量: ${notes.total}")
        appendLine("总DX分: $maxDeluxeScore")
        appendLine("谱师: $notesDesigner")
    }.trim().newLine()

    fun infoMD(
        jacketUrl: String
    ) = MarkdownTemplates.Templates.chart(this, "$jacketUrl/${music.resourceId}.jpg")
}
