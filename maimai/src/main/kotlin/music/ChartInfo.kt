package xyz.xszq.bot.music

import io.ktor.http.*
import korlibs.io.util.toStringDecimal
import xyz.xszq.bot.component.MarkdownTemplates.href
import xyz.xszq.bot.message.Image
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.newLine
import xyz.xszq.bot.payload.markdown.MarkdownData
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
        if (fitLevelValue != 0.0)
            appendLine("拟合定数: ${fitLevelValue.toStringDecimal(1)}")
    }.trim().newLine()

    fun infoMD(
        jacketUrl: String
    ) = Markdown(MarkdownData(buildString {
        appendLine("![img#190px #190px]($jacketUrl/${music.resourceId}.jpg)")
        appendLine("**${difficulty.names.last()}${music.id}. ${music.name}**")
        appendLine()
        appendLine("> **曲师:** ${href("曲师查歌 ${music.artist}", music.artist)}")
        appendLine("> **分类:** ${href("${music.genre.genreName}有什么歌", music.genre.genreName)}")
        appendLine("> **版本:** ${href("版本查歌 ${music.version.name}", music.version.name)}")
        appendLine("> **BPM:** ${href("BPM查歌 ${music.bpm}", music.bpm.toString())}")
        appendLine("> **等级:** $level (${href("定数查歌 $levelValue", levelValue.toString())})")
        appendLine("> **TAP:** ${notes.tap}")
        appendLine("> **HOLD:** ${notes.hold}")
        appendLine("> **SLIDE:** ${notes.slide}")
        appendLine("> **BREAK:** ${notes.`break`}")
        if (notes.touch != 0)
            appendLine("> **TOUCH:** ${notes.touch}")
        appendLine("> **总物量:** ${notes.total}")
        appendLine("> **总DX分:** $maxDeluxeScore")
        val designer = notesDesigner.ifBlank { "-" }
        appendLine("> **谱师:** ${href("谱师查歌 $designer", designer)}")
        if (fitLevelValue != 0.0) {
            val value = fitLevelValue.toStringDecimal(1)
            appendLine("> **拟合定数:** ${href("拟合定数查歌 $value", value)}")
        }
    }))
}
