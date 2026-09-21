package xyz.xszq.bot.chunithm.component

import io.ktor.http.*
import xyz.xszq.bot.chunithm.Chunithm
import xyz.xszq.bot.chunithm.music.ChartInfo
import xyz.xszq.bot.chunithm.music.MusicDifficulty
import xyz.xszq.bot.chunithm.music.MusicInfo
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.payload.markdown.MarkdownData


/**
 * Markdown 模板
 */
object MarkdownTemplates {
    /**
     * 封面图路径
     */
    lateinit var jacketUrl: String

    /**
     * 初始化
     *
     * @param maimai 中二插件
     */
    fun init(chunithm: Chunithm) {
        jacketUrl = chunithm.config.tokens["assets-jacket"] ?: throw Exception("assets-jacket missing")
    }

    /**
     * Markdown 按钮
     */
    object Keyboards {
        /**
         * 分页按钮
         *
         * @param button 按钮 ID
         * @param keyword 关键词
         * @param nowPage 当前页码
         * @param totalPages 总页数
         * @return 按钮
         */
        fun selectPaged(button: String, keyword: String, nowPage: Int = 1, totalPages: Int = 1) = Keyboard.create {
            row {
                if (nowPage > 1)
                    callback("⬅️上一页", "$keyword\n${nowPage - 1}", id = button)
                if (nowPage < totalPages)
                    callback("➡️下一页", "$keyword\n${nowPage + 1}", id = button)
            }
        }
    }

    /**
     * Markdown 模板
     */
    object Templates {
        /**
         * 选择歌曲的列表
         *
         * @param title 列表标题
         * @param type 命令名
         * @param keyword 查询关键词
         * @param difficulty 谱面难度
         * @param result 歌曲列表
         * @param displayName 命令前缀
         * @param nowPage 当前页码
         * @param totalPages 总页数
         */
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
                val url = "$jacketUrl/${music.resourceId}_s.jpg"
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

        /**
         * 选择谱面的列表
         *
         * @param title 列表标题
         * @param type 命令名
         * @param keyword 查询关键词
         * @param result 谱面列表
         * @param displayName 命令前缀
         * @param nowPage 当前页码
         * @param totalPages 总页数
         */
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
    /**
     * Markdown 链接
     *
     * @param link 命令文本
     * @param show 显示文本
     * @param enter 点击后是否直接发送命令
     * @return Markdown 文本
     */
    fun href(
        link: String,
        show: String,
        enter: Boolean = true
    ) = "[${show.markdownEscape()}](mqqapi://aio/inlinecmd?command=${link.encodeURLParameter()}&enter=${enter}&reply=false)"
    private fun String.markdownEscape() =
        replace("[", "\\[").replace("]", "\\]")
}