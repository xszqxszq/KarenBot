package xyz.xszq.bot.chunithm.music

import korlibs.io.file.VfsFile
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.serialization.Serializable
import xyz.xszq.bot.chunithm.component.MarkdownTemplates.href
import xyz.xszq.bot.message.Image
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.newLine
import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.payload.markdown.MarkdownData
import xyz.xszq.bot.payload.markdown.RenderData
import xyz.xszq.bot.plus

@Suppress("unused")
@Serializable
class MusicInfo(
    val id: Int,
    val title: String,
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
    val resourceId: Int
        get() = when {
            isWordsEnd -> charts.first().originId ?: 0
            else -> id
        }
    val name: String
        get() = when {
            isWordsEnd -> {
                val chart = charts.first()
                "[${chart.kanji}]$title"
            }
            else -> title
        }
    val isWordsEnd
        get() = charts.all { it.difficulty == MusicDifficulty.WorldsEnd }
    suspend fun cover(): VfsFile {
        val cover = localCurrentDirVfs["$BASEDIR/$resourceId.png"]
        if (!cover.exists() || !cover.isFile()) {
            return localCurrentDirVfs["$BASEDIR/0.png"]
        }
        return cover
    }

    suspend fun infoText() = Image(cover()) + buildString {
        appendLine("${id}. $name")
        appendLine("曲师: $artist")
        appendLine("分类: ${genre.genreName}")
        appendLine("版本: ${version.name}${if (isNew) " (计入n20)" else ""}")
        appendLine("BPM: $bpm")
        if (!isWordsEnd)
            appendLine("定数: ${charts.joinToString("/") { chart -> chart.levelValue.toString() }}")
        appendLine("谱师: ${charts.joinToString("/") { chart -> chart.notesDesigner.ifBlank { "-" } }}")
    }.trim().newLine()


    fun infoMD(
        jacketUrl: String
    ) = Markdown(MarkdownData(buildString {
        appendLine("![img#190px #190px]($jacketUrl/${resourceId}.jpg)")
        appendLine("**${id}. ${name}**")
        appendLine("**曲师:** ${href("/chu 曲师查歌 $artist", artist)}")
        appendLine("**分类:** ${href("/chu ${genre.genreName}有什么歌", genre.genreName)}")
        appendLine("**版本:** ${href("/chu 版本查歌 ${version.name}", version.name)}")
        appendLine("**BPM:** ${href("/chu BPM查歌 $bpm", bpm.toString())}")
        if (!isWordsEnd)
            appendLine("**定数:** ${
                charts.joinToString("/") {
                    href("/chu 定数查歌 ${it.levelValue}", it.levelValue.toString())
                }
            }")
        appendLine("**谱师:** ${
            charts.joinToString("/") {
                val designer = it.notesDesigner.ifBlank { "-" }
                if (designer != "-") {
                    href("/chu 谱师查歌 $designer", designer)
                } else designer
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
                at(display, "/chu ${chart.difficulty.brief}${id}", enter = true, id = "level")
            }
        }
        row {
            at("🔊试听一下", "/chu 预览id$id", enter = true, style = RenderData.GRAY, id = "3")
//            at("➕添加别名", "/chu 添加别名 $id", style = RenderData.GRAY, id = "4")
        }
    })

    companion object {
        private const val BASEDIR = "./data/chunithm/covers"

        private val MusicDifficulty.emoji: String
            get() = when(this) {
                MusicDifficulty.Basic -> "\uD83D\uDFE9"
                MusicDifficulty.Advanced -> "\uD83D\uDFE8"
                MusicDifficulty.Expert -> "\uD83D\uDFE5"
                MusicDifficulty.Master -> "\uD83D\uDFEA"
                MusicDifficulty.Ultima -> "⬛"
                MusicDifficulty.WorldsEnd -> "\uD83C\uDF08"
            }
    }
}