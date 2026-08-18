package xyz.xszq.bot.chunithm.component

import io.ktor.http.*
import xyz.xszq.bot.chunithm.Chunithm
import xyz.xszq.bot.chunithm.music.ChartInfo
import xyz.xszq.bot.chunithm.music.MusicDifficulty
import xyz.xszq.bot.chunithm.music.MusicInfo
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.payload.markdown.MarkdownData


object MarkdownTemplates {
    lateinit var jacketUrl: String

    fun init(chunithm: Chunithm) {
        jacketUrl = chunithm.config.tokens["assets-jacket"] ?: throw Exception("assets-jacket missing")
    }

    object Keyboards {
        fun selectPaged(button: String, keyword: String, nowPage: Int = 1, totalPages: Int = 1) = Keyboard.create {
            row {
                if (nowPage > 1)
                    callBack("⬅️上一页", "$keyword\n${nowPage - 1}", id = button)
                if (nowPage < totalPages)
                    callBack("➡️下一页", "$keyword\n${nowPage + 1}", id = button)
            }
        }
    }

    object Templates {
        fun selectMusic(
            title: String,
            type: String,
            keyword: String,
            difficulty: MusicDifficulty?,
            result: List<MusicInfo>,
            displayName: String? = null,
            nowPage: Int = 1,
            totalPages: Int = 1
        ): Markdown {
            val rows = result.take(10).joinToString("\n") { music ->
                val url = "$jacketUrl/${music.resourceId}.jpg"
                val command = "/chu " + "${displayName ?: type} ${difficulty?.brief ?: ""}id${music.id}".trim()
                val musicName = "${music.id}. ${music.name}"
                "![preview #20px #20px]($url) ${href(command, musicName)}"
            }

            val data = MarkdownData("**$title**\n\n$rows")
            val keyboard =
                if (totalPages == 1) null
                else Keyboards.selectPaged(type, keyword, nowPage, totalPages)
            return Markdown(data, keyboard)
        }

        fun selectChart(
            title: String,
            type: String,
            keyword: String,
            result: List<ChartInfo>,
            displayName: String? = null,
            nowPage: Int = 1,
            totalPages: Int = 1
        ): Markdown {
            val rows = result.take(10).joinToString("\n") { chart ->
                val url = "$jacketUrl/${chart.music.resourceId}.jpg"
                val command = "/chu " + "${displayName ?: type} ${chart.difficulty.brief}id${chart.music.id}".trim()
                val chartName = "${chart.difficulty.brief}${chart.music.id}. ${chart.music.name}"
                "![preview #20px #20px]($url) ${href(command, chartName)}"
            }

            val data = MarkdownData("**$title**\n\n$rows")
            val keyboard =
                if (totalPages == 1) null
                else Keyboards.selectPaged(type, keyword, nowPage, totalPages)
            return Markdown(data, keyboard)
        }
    }
    fun href(
        link: String,
        show: String,
        enter: Boolean = true
    ) = "[${show.markdownEscape()}](mqqapi://aio/inlinecmd?command=${link.encodeURLParameter()}&enter=${enter}&reply=false)"
    private fun String.markdownEscape() =
        replace("[", "\\[").replace("]", "\\]")
}