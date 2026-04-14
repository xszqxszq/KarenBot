package xyz.xszq.bot.maimai.music

import korlibs.io.file.VfsFile
import korlibs.io.file.std.localCurrentDirVfs
import korlibs.io.util.toStringDecimal
import xyz.xszq.bot.maimai.component.MarkdownTemplates.href
import xyz.xszq.bot.message.Image
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.newLine
import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.payload.markdown.MarkdownData
import xyz.xszq.bot.payload.markdown.RenderData
import xyz.xszq.bot.plus

class MusicInfo(
    val id: Int,
    val name: String,
    val type: MusicType,
    @Suppress("unused")
    val rights: String,
    val artist: String,
    val genre: MusicGenre,
    val bpm: Int,
    val version: GameVersion,
    val isNew: Boolean
) {
    var charts: List<ChartInfo> = listOf()
    val resourceId: Int
        get() = id % 10000
    suspend fun cover(): VfsFile {
        val cover = localCurrentDirVfs["$BASEDIR/$resourceId.png"]
        if (!cover.exists() || !cover.isFile()) {
            return localCurrentDirVfs["$BASEDIR/0.png"]
        }
        return cover
    }
    val fakeReMaster: ChartInfo
        get() = ChartInfo(this, MusicDifficulty.ReMaster, "", 0.0, Notes(), "")

    suspend fun infoText() = Image(cover()) + buildString {
        appendLine("${id}. $name")
        appendLine("艺术家: $artist")
        appendLine("分类：${genre.genreName}")
        appendLine("版本：${version.name}${if (isNew) " (计入b15)" else ""}")
        appendLine("BPM：${bpm}")
        appendLine("定数：${charts.joinToString("/") { it.levelValue.toString() }}")
        appendLine("拟合定数：${charts.joinToString("/") { 
            val value = it.fitLevelValue.toStringDecimal(1)
            if (value == "0.0") "-" else value
        }}")
        appendLine("谱师：${charts.joinToString("/") {
            it.notesDesigner.let { d ->
                d.ifBlank { "-" }
            }
        }}")
    }.trim().newLine()

    fun infoMD(
        jacketUrl: String
    ) = Markdown(MarkdownData(buildString {
        appendLine("![img#190px #190px]($jacketUrl/${resourceId}.jpg)")
        appendLine("**${id}. ${name}**")
        appendLine("**曲师:** ${href("/mai 曲师查歌 $artist", artist)}")
        appendLine("**分类:** ${href("/mai ${genre.genreName}有什么歌", genre.genreName)}")
        appendLine("**版本:** ${href("/mai 版本查歌 ${version.name}", version.name)}")
        appendLine("**BPM:** ${href("/mai BPM查歌 $bpm", bpm.toString())}")
        appendLine("**定数:** ${
            charts.joinToString("/") {
                href("/mai 定数查歌 ${it.levelValue}", it.levelValue.toString())
            }
        }")
        appendLine("**谱师:** ${
            charts.joinToString("/") {
                val designer = it.notesDesigner.ifBlank { "-" }
                if (designer != "-") {
                    href("/mai 谱师查歌 $designer", designer)
                } else designer
            }
        }")
        appendLine("**拟合定数:** ${
            charts.joinToString("/") {
                if (it.fitLevelValue == 0.0) {
                    "-"
                } else {
                    val value = it.fitLevelValue.toStringDecimal(1)
                    href("/mai 拟合定数查歌 $value", value)
                }
            }
        }")
    }), Keyboard.create {
        row {
            charts.forEach { chart ->
                val emoji = chart.difficulty.emoji
                val display = if (charts.size < 5)
                    "$emoji${chart.difficulty.brief}"
                else
                    emoji
                at(display, "/mai ${chart.difficulty.brief}${id}", enter = true, id = "level")
            }
        }
        row {
            at("💯查成绩", "/mai info $id", enter = true, id = "1")
            at("📜歌50", "/mai 歌50 $id", enter = true, id = "2")
        }
        row {
            at("🔊试听一下", "/mai 预览id$id", enter = true, style = RenderData.GRAY, id = "3")
            at("➕添加别名", "/mai 添加别名 $id", style = RenderData.GRAY, id = "4")
        }
    })

    companion object {
        private const val BASEDIR = "./data/maimai/covers"
        private val MusicDifficulty.emoji: String
            get() = when(this) {
                MusicDifficulty.Basic -> "\uD83D\uDFE9"
                MusicDifficulty.Advanced -> "\uD83D\uDFE8"
                MusicDifficulty.Expert -> "\uD83D\uDFE5"
                MusicDifficulty.Master -> "\uD83D\uDFEA"
                MusicDifficulty.ReMaster -> "⬜"
                MusicDifficulty.Utage -> "\uD83D\uDFEB"
            }
    }
}