package xyz.xszq.bot.music

import korlibs.io.file.VfsFile
import korlibs.io.file.std.localCurrentDirVfs
import korlibs.io.util.toStringDecimal
import xyz.xszq.bot.MarkdownTemplates
import xyz.xszq.bot.message.Image
import xyz.xszq.bot.newLine
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
    companion object {
        private const val BASEDIR = "./data/maimai/covers"
    }

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
    ) = MarkdownTemplates.Templates.music(this, "$jacketUrl/${resourceId}.jpg")
}