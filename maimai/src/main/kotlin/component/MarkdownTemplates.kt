package xyz.xszq.bot.component

import io.ktor.http.*
import xyz.xszq.bot.Maimai
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.music.ChartInfo
import xyz.xszq.bot.music.MusicDifficulty
import xyz.xszq.bot.music.MusicInfo
import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.payload.markdown.MarkdownData
import xyz.xszq.bot.payload.markdown.RenderData

object MarkdownTemplates {
    lateinit var jacketUrl: String

    fun init(maimai: Maimai) {
        jacketUrl = maimai.config.tokens["assets-jacket"] ?: throw Exception("assets-jacket missing")
    }

    object Keyboards {
        fun single(data: String, label: String, enter: Boolean = false) = Keyboard.create {
            row {
                at(label, data, enter)
            }
        }

        fun selectPaged(button: String, keyword: String, nowPage: Int = 1, totalPages: Int = 1) = Keyboard.create {
            row {
                if (nowPage > 1)
                    callBack("⬅️上一页", "$keyword\n${nowPage - 1}", enter = true, id = button)
                if (nowPage < totalPages)
                    callBack("➡️下一页", "$keyword\n${nowPage + 1}", enter = true, id = button)
            }
        }
    }

    object Templates {
        fun brief(
            title: String,
            content: String
        ) = MarkdownData(buildString {
            appendLine("**$title**")
            appendLine()
            append(content)
        })

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
                val textAttr = "${displayName ?: type} ${difficulty?.brief ?: ""}id${music.id}"
                    .trim().encodeURLParameter()
                val showAttr = "${music.id}. ${music.name}".encodeURLParameter()
                "![preview #20px #20px]($url) <qqbot-cmd-input text=\"$textAttr\" show=\"$showAttr\" reference=\"false\"/>"
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
                val textAttr = "${displayName ?: type} ${chart.difficulty.brief}id${chart.music.id}"
                    .trim().encodeURLParameter()
                val showAttr = "${chart.difficulty.brief}${chart.music.id}. ${chart.music.name}".encodeURLParameter()
                "![preview #20px #20px]($url) <qqbot-cmd-input text=\"$textAttr\" show=\"$showAttr\" reference=\"false\"/>"
            }

            val data = MarkdownData("**$title**\n\n$rows")
            val keyboard =
                if (totalPages == 1) null
                else Keyboards.selectPaged(type, keyword, nowPage, totalPages)
            return Markdown(data, keyboard)
        }

        fun selectBackends(message: String) = Markdown(
            brief("舞萌DX", "您还未在查分器上绑定QQ号。请选择一个查分器来绑定您的QQ号："),
            Keyboard.create {
                row {
                    link("水鱼查分器", "https://otmdb.cn/jump/maimaidxprober", id = "1")
                    link("落雪查分器", "https://otmdb.cn/jump/lxnsprober", id = "2")
                }
                row {
                    at("点我重试", message, enter = true, style = RenderData.FILLED_BLUE, id = "3")
                }
            }
        )
    }
}