package xyz.xszq.bot.maimai.music

import korlibs.io.util.toStringDecimal
import xyz.xszq.bot.maimai.component.MarkdownTemplates.href
import xyz.xszq.bot.message.Image
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.newLine
import xyz.xszq.bot.payload.markdown.Keyboard
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
        appendLine("曲师: ${music.artist}")
        appendLine("分类: ${music.genre.genreName}")
        appendLine("版本: ${music.version.name}")
        appendLine("BPM: ${music.bpm}")
        appendLine("等级: $level (${levelValue})")
        appendLine("谱师: $notesDesigner")
        if (fitLevelValue != 0.0)
            appendLine("拟合定数: ${fitLevelValue.toStringDecimal(1)}")
        appendLine("TAP: ${notes.tap}")
        appendLine("HOLD: ${notes.hold}")
        appendLine("SLIDE: ${notes.slide}")
        appendLine("BREAK: ${notes.`break`}")
        if (notes.touch != 0)
            appendLine("TOUCH: ${notes.touch}")
        appendLine("总物量: ${notes.total}")
        appendLine("总DX分: $maxDeluxeScore")
    }.trim().newLine()

    fun infoMD(
        jacketUrl: String
    ) = Markdown(MarkdownData(buildString {
        appendLine("![img#190px #190px]($jacketUrl/${music.resourceId}.jpg)")
        appendLine("**${difficulty.names.last()}${music.id}. ${music.name}**")
        appendLine("**曲师:** ${href("/mai 曲师查歌 ${music.artist}", music.artist)}")
        appendLine("**分类:** ${href("/mai ${music.genre.genreName}有什么歌", music.genre.genreName)}")
        appendLine("**版本:** ${href("/mai 版本查歌 ${music.version.name}", music.version.name)}")
        appendLine("**BPM:** ${href("/mai BPM查歌 ${music.bpm}", music.bpm.toString())}")
        appendLine("**等级:** $level (${href("/mai 定数查歌 $levelValue", levelValue.toString())})")
        val designer = notesDesigner.ifBlank { "-" }
        appendLine("**谱师:** ${href("/mai 谱师查歌 $designer", designer)}")
        if (fitLevelValue != 0.0) {
            val value = fitLevelValue.toStringDecimal(1)
            appendLine("**拟合定数:** ${href("/mai 拟合定数查歌 $value", value)}")
        }
        appendLine()
        appendLine("> **TAP:** ${notes.tap}")
        appendLine("> **HOLD:** ${notes.hold}")
        appendLine("> **SLIDE:** ${notes.slide}")
        appendLine("> **BREAK:** ${notes.`break`}")
        if (notes.touch != 0)
            appendLine("> **TOUCH:** ${notes.touch}")
        appendLine("> **总物量:** ${notes.total}")
        appendLine("> **总DX分:** $maxDeluxeScore")
    }), Keyboard.create {
        row {
            link("谱面确认", "https://otmdb.cn/jump/maimai_chart?chart_id=${
                if (difficulty == MusicDifficulty.Utage)
                    music.resourceId + 100000
                else
                    music.resourceId
            }&difficulty=${
                if (difficulty == MusicDifficulty.Utage)
                    0
                else
                    difficulty.value
            }")
        }
    })
}