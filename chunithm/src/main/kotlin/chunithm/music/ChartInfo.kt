package xyz.xszq.bot.chunithm.music

import kotlinx.serialization.Serializable
import xyz.xszq.bot.chunithm.component.MarkdownTemplates.href
import xyz.xszq.bot.message.Image
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.newLine
import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.payload.markdown.MarkdownData
import xyz.xszq.bot.plus

@Serializable
class ChartInfo(
    val music: MusicInfo,
    val difficulty: MusicDifficulty,
    val level: String,
    val levelValue: Double,
    val notes: Notes,
    val notesDesigner: String,
    val kanji: String ?= null,
    val star: Int ?= null,
    val originId: Int ?= null
) {
    var origin: MusicInfo ?= null

    suspend fun infoText() = Image(music.cover()) + buildString {
        appendLine("${difficulty.brief}${music.id}. ${music.name}")
        appendLine("曲师: ${music.artist}")
        appendLine("分类: ${music.genre.genreName}")
        appendLine("版本: ${music.version.name}")
        appendLine("BPM: ${music.bpm}")
        if (difficulty != MusicDifficulty.WorldsEnd)
            appendLine("等级: $level (${levelValue})")
        appendLine("谱师: ${notesDesigner.ifBlank { "-" }}")
        star ?.let {
            appendLine("星数: $star")
        }
        appendLine("TAP: ${notes.tap}")
        appendLine("HOLD: ${notes.hold}")
        appendLine("SLIDE: ${notes.slide}")
        appendLine("AIR: ${notes.air}")
        appendLine("FLICK: ${notes.flick}")
        appendLine("总物量: ${notes.total}")
    }.trim().newLine()

    fun infoMD(
        jacketUrl: String
    ) = Markdown(MarkdownData(buildString {
        appendLine("![img#190px #190px]($jacketUrl/${music.resourceId}.jpg)")
        appendLine("**${difficulty.names.last()}${music.id}. ${music.name}**")
        appendLine("**曲师:** ${href("/chu 曲师查歌 ${music.artist}", music.artist)}")
        appendLine("**分类:** ${href("/chu ${music.genre.genreName}有什么歌", music.genre.genreName)}")
        appendLine("**版本:** ${href("/chu 版本查歌 ${music.version.name}", music.version.name)}")
        appendLine("**BPM:** ${href("/chu BPM查歌 ${music.bpm}", music.bpm.toString())}")
        appendLine("**等级:** $level (${href("/chu 定数查歌 $levelValue", levelValue.toString())})")
        val designer = notesDesigner.ifBlank { "-" }
        appendLine("**谱师:** ${href("/chu 谱师查歌 $designer", designer)}")
        star ?.let {
            appendLine("**星数:** $star")
        }
        appendLine()
        appendLine("> **TAP:** ${notes.tap}")
        appendLine("> **HOLD:** ${notes.hold}")
        appendLine("> **SLIDE:** ${notes.slide}")
        appendLine("> **AIR:** ${notes.air}")
        appendLine("> **FLICK:** ${notes.flick}")
        appendLine("> **总物量:** ${notes.total}")
    }))
}