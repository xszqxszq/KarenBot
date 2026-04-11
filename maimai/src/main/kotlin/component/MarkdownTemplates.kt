package xyz.xszq.bot.component

import io.ktor.http.*
import korlibs.io.util.toStringDecimal
import xyz.xszq.bot.Maimai
import xyz.xszq.bot.api.DivingFish
import xyz.xszq.bot.api.LXNS
import xyz.xszq.bot.api.MaimaiAPI
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.music.ChartInfo
import xyz.xszq.bot.music.MusicDifficulty
import xyz.xszq.bot.music.MusicInfo
import xyz.xszq.bot.payload.markdown.*

object MarkdownTemplates {
    lateinit var jacketUrl: String

    fun init(maimai: Maimai) {
        jacketUrl = maimai.config.tokens["assets-jacket"] ?: throw Exception("assets-jacket missing")
    }

    object Keyboards {
        fun single(data: String, label: String, enter: Boolean = false) = Keyboard.create {
            row {
                atButton(label, data, enter)
            }
        }

        fun music(music: MusicInfo) = Keyboard.create {
            row {
                music.charts.forEach { chart ->
                    val emoji = chart.difficulty.emoji
                    val display = if (music.charts.size < 5)
                        "$emoji${chart.difficulty.brief}"
                    else
                        emoji
                    atButton(display, "${chart.difficulty.brief}${music.id}", enter = true, id = "level")
                }
            }
            row {
                atButton("💯查成绩", "info ${music.id}", enter = true, id = "1")
                atButton("📜歌50", "歌50 ${music.id}", enter = true, id = "2")
            }
            row {
                atButton("🔊试听一下", "预览id${music.id}", enter = true, style = RenderData.GRAY, id = "3")
                atButton("➕添加别名", "添加别名 ${music.id}", style = RenderData.GRAY, id = "4")
            }
        }

        fun oauth(authUrl: String) = Keyboard.create {
            row { linkButton("点我授权", authUrl) }
        }

        fun collection(type: String, engType: String) = Keyboard.create {
            row {
                linkButton("选择$type", "https://otmdb.cn/bot/maimai/$engType", id = "1")
                atButton("⚙ 设置$type", "设置$type ", id = "2")
            }
        }

        fun queue(now: String? = null) = Keyboard.create {
            row {
                atButton("查询人数", "/j", enter = true, id = "")
                now?.let {
                    atButton("添加别名", "/排卡管理 添加别名 $it 这里填别名", id = "")
                }
                atButton("更新人数", now ?: "\n", id = "")
            }
        }

        fun image(command: String, nowPage: Int? = null, totalPages: Int? = null) = Keyboard.create {
            row {
                atButton("💯我也要查", command, id = "1")
                linkButton("随心配", "https://otmdb.cn/bot/maimai/combo", enter = true, id = "2")
                atButton("🎨修改设置", "设置mai", enter = true, id = "3")
            }
            nowPage?.let {
                if (totalPages == null || totalPages <= 1) return@let
                row {
                    if (nowPage > 1)
                        atButton("⬅️上一页", "$command ${nowPage - 1}", enter = true, id = "4")
                    if (nowPage < totalPages)
                        atButton("➡️下一页", "$command ${nowPage + 1}", enter = true, id = "5")
                }
            }
        }

        fun importData(backend: MaimaiAPI) = Keyboard.create {
            when (backend) {
                is DivingFish -> row {
                    linkButton("🐟水鱼(电脑/iOS)", "https://otmdb.cn/jump/maimaidxprober_import", id = "1")
                }
                is LXNS -> row {
                    linkButton("❄落雪(电脑/手机)", "https://otmdb.cn/jump/lxnsprober_import", id = "2")
                }
            }
            row {
                linkButton("🐇UsagiPass(iOS/安卓)", "https://otmdb.cn/jump/maimai_prober_mobile", id = "3")
                linkButton("🤖可怜BOT(安卓)", "https://bot-docs.otmdb.cn/maimai/update", id = "4")
            }
        }

        fun tryIt(command: String) = single(command.trim() + " ", "⬇试一试")
        fun aliases(command: String) = single(command.trim(), "添加别名")
        fun aliasVote(command: String) = single(command.trim(), "点我投票", enter = true)

        fun selectPaged(button: String, keyword: String, nowPage: Int = 1, totalPages: Int = 1) = Keyboard.create {
            row {
                if (nowPage > 1)
                    callbackButton("⬅️上一页", "$keyword\n${nowPage - 1}", enter = true, id = button)
                if (nowPage < totalPages)
                    callbackButton("➡️下一页", "$keyword\n${nowPage + 1}", enter = true, id = button)
            }
        }

        fun backends(message: String) = Keyboard.create {
            row {
                linkButton("水鱼查分器", "https://otmdb.cn/jump/maimaidxprober", id = "1")
                linkButton("落雪查分器", "https://otmdb.cn/jump/lxnsprober", id = "2")
            }
            row {
                atButton("点我重试", message, enter = true, style = RenderData.FILLED_BLUE, id = "3")
            }
        }

        val BIND_QQ = Keyboard.create {
            row {
                atButton("⬇点我输入", "/bind ", id = "1")
            }
        }
        val BIND_DF = Keyboard.create {
            row {
                atButton("⬇点我输入", "/绑定水鱼 ", id = "1")
            }
        }
        val HELP_PROXY = Keyboard.create {
            row {
                linkButton("设置代理", "https://bot-docs.otmdb.cn/maimai/update", id = "1")
            }
        }
        val UPDATE = Keyboard.create {
            row {
                atButton("点击更新", "更新", enter = true, id = "1")
            }
        }
        val USER_EULA = Keyboard.create {
            row {
                linkButton("前往查分器", "https://otmdb.cn/jump/maimaidxprober", id = "1")
            }
        }

        val HELP = Keyboard.create {
            row {
                atButton("🔎查歌", "牛奶歌是什么歌", id = "1")
                atButton("📋单曲成绩", "info 海底谭", id = "1")
            }
            row {
                atButton("💯Best50", "b50", id = "1")
                linkButton("随心配50", "https://otmdb.cn/bot/maimai/combo", id = "1")
            }
            row {
                atButton("⏳完成表", "橙将完成表", id = "1")
                atButton("📖分数列表", "13分数列表", id = "1")
            }
            row {
                atButton("🕹️开字母", "舞萌开字母", id = "1")
                linkButton("更多功能...", "https://otmdb.cn/bot/maimai", id = "1")
            }
        }

        val SETTINGS = Keyboard.create {
            row {
                atButton("👤设置头像", "设置头像", enter = true, id = "1")
                atButton("📰设置牌子", "设置牌子", enter = true, id = "1")
            }
            row {
                atButton("🐟使用水鱼查分", "设置查分器 水鱼", enter = true, id = "1")
                atButton("❄使用落雪查分", "设置查分器 落雪", enter = true, id = "1")
            }
            row {
                atButton("🔄自动选择查分器", "设置查分器 自动", enter = true, id = "1")
            }
        }

        val QUEUE_INIT = Keyboard.create {
            row {
                atButton("添加机厅", "/排卡管理 添加机厅 机厅名称", id = "")
            }
        }

        val QUEUE_HELP = Keyboard.create {
            row {
                atButton("查询人数", "几", enter = true, id = "")
                atButton("添加机厅", "/排卡管理 添加机厅 机厅名称", id = "")
                atButton("删除机厅", "/排卡管理 删除机厅 机厅名称", id = "")
            }
            row {
                atButton("查看别名", "/排卡管理 查看别名 机厅名称", id = "")
                atButton("添加别名", "/排卡管理 添加别名 机厅名称 别名名称", id = "")
                atButton("删除别名", "/排卡管理 删除别名 机厅名称 别名名称", id = "")
            }
        }

        val QUEUE_UPDATE = Keyboard.create {
            row {
                atButton("更新人数", "\n", id = "")
            }
        }

        val GUESS = Keyboard.create {
            row {
                atButton("⬇输入答案", " ", style = RenderData.FILLED_BLUE, id = "1")
            }
            row {
                atButton("不玩了", "不玩了", enter = true, style = RenderData.RED, id = "2")
            }
        }

        val GUESS_AGAIN = Keyboard.create {
            row {
                atButton("🕹️再玩一把", "猜歌", enter = true, id = "1")
            }
        }
        val GUESS_OPEN_AGAIN = Keyboard.create {
            row {
                atButton("🕹️再玩一把", "舞萌开字母", enter = true, id = "1")
            }
        }
    }

    object Templates {
        fun music(music: MusicInfo, cover: String) = Markdown(MarkdownData(buildString {
            appendLine("![img#190px #190px]($cover)")
            appendLine("**${music.id}. ${music.name}**")
            appendLine("**曲师:** ${music.artist}")
            appendLine("**分类:** ${music.genre.genreName}")
            appendLine("**版本:** ${music.version.name}")
            appendLine("**BPM:** ${music.bpm}")
            appendLine("**定数:** ${music.charts.joinToString("/") { it.levelValue.toString() }}")
            appendLine("**拟合定数:** ${music.charts.joinToString("/") {
                if (it.fitLevelValue == 0.0) "-" else it.fitLevelValue.toStringDecimal(1)
            }}")
            append("**谱师:** ${music.charts.joinToString("/") {
                val designer = it.notesDesigner.ifBlank { "-" }
                if (designer != "-") {
                    val command = "谱师查歌 $designer".encodeURLParameter()
                    "<qqbot-cmd-input text=\"$command\" show=\"${designer.encodeURLParameter()}\" reference=\"false\"/>"
                } else designer
            }}")
        }), Keyboards.music(music))

        fun chart(chart: ChartInfo, cover: String): Markdown {
            var designer = chart.notesDesigner.ifBlank { "-" }
            if (designer != "-") {
                val command = "谱师查歌 $designer".encodeURLParameter()
                designer = "<qqbot-cmd-input text=\"$command\" show=\"${designer.encodeURLParameter()}\" reference=\"false\"/>"
            }

            val fitLevelStr =
                if (chart.fitLevelValue != 0.0) "\n**拟合定数:** ${chart.fitLevelValue.toStringDecimal(1)}"
                else ""
            val touchStr =
                if (chart.notes.touch != 0) "\n**TOUCH:** ${chart.notes.touch}"
                else ""

            return Markdown(MarkdownData(buildString {
                appendLine("![img#190px #190px]($cover)")
                appendLine("**${chart.difficulty.names.last()}${chart.music.id}. ${chart.music.name}**")
                appendLine("**等级:** ${chart.level} (${chart.levelValue})")
                appendLine("**TAP:** ${chart.notes.tap}")
                appendLine("**HOLD:** ${chart.notes.hold}")
                appendLine("**SLIDE:** ${chart.notes.slide}")
                appendLine("**BREAK:** ${chart.notes.`break`}$touchStr")
                appendLine("**总物量:** ${chart.notes.total}")
                appendLine("**总DX分:** ${chart.maxDeluxeScore}")
                append("**谱师:** $designer$fitLevelStr")
            }))
        }

        fun oauth(authUrl: String) = Markdown(MarkdownData(buildString {
            appendLine("**请求授权**")
            appendLine()
            append("使用该功能时，需要您授权BOT访问您在落雪查分器的全部成绩信息，请点击下方登录并授权：")
        }), Keyboards.oauth(authUrl))

        fun queue(title: String, content: String, now: String? = null) = Markdown(MarkdownData(buildString {
            appendLine("**$title**")
            appendLine()
            append(content)
        }), Keyboards.queue(now))

        fun queueInit(title: String, content: String) = Markdown(MarkdownData(buildString {
            appendLine("**$title**")
            appendLine()
            append(content)
        }), Keyboards.QUEUE_INIT)

        fun queueUpdate(info: String) = Markdown(MarkdownData(buildString {
            appendLine("**机厅排卡人数：**")
            appendLine()
            appendLine(info.split("\n").joinToString("\n") { "> $it" })
            appendLine("> ")
            appendLine("> 更新数据请使用“机厅名+数量”。")
            appendLine("\t例：某某机厅3")
            appendLine("\t例：机厅+1")
            append("\t例：jt-2")
        }), Keyboards.QUEUE_UPDATE)

        fun image(
            url: String,
            size: Pair<Int, Int>,
            command: String,
            description: String?,
            nowPage: Int? = null,
            totalPages: Int? = null
        ) = Markdown(MarkdownData(buildString {
            appendLine("**查询结果**")
            appendLine()
            appendLine("![img #${size.first}px #${size.second}px]($url)")
            appendLine()
            append(description ?: "")
        }), Keyboards.image(command, nowPage, totalPages))

        fun brief(
            title: String,
            content: String
        ) = MarkdownData(buildString {
            appendLine("**$title**")
            appendLine()
            append(content)
        })

        fun guess(
            hint: String
        ) = brief("maimai 猜歌", hint).toMessage(Keyboards.GUESS)

        fun guessImage(
            url: String,
            description: String
        ) = MarkdownData(buildString {
            appendLine("**maimai 猜歌**")
            appendLine()
            appendLine("![img #300px #300px]($url)")
            appendLine()
            append(description)
        })

        fun importData(
            backend: MaimaiAPI
        ) = brief("舞萌DX", "您似乎尚未导入舞萌DX分数到查分器，请参考下方教程：")
            .toMessage(Keyboards.importData(backend))

        fun guessCropped(
            url: String,
            description: String
        ) = guessImage(url, description).toMessage(Keyboards.GUESS)

        fun guessFinished(
            url: String,
            hint: String
        ) = guessImage(url, hint).toMessage(Keyboards.GUESS_AGAIN)

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
            Keyboards.backends(message)
        )

        val BIND_QQ = brief("舞萌DX", "为了继续后续查询，请输入您的QQ号来绑定：").toMessage(Keyboards.BIND_QQ)

        val USER_EULA = brief("舞萌DX", "请前往查分器同意用户协议再进行查询：").toMessage(Keyboards.USER_EULA)

        val HELP = brief("舞萌DX", buildString {
            appendLine("这是一个查询舞萌DX成绩及相关信息的功能。")
            append("支持以下功能指令：")
        }).toMessage(Keyboards.HELP)

        val SETTINGS = brief("功能设置", "支持以下设定：").toMessage(Keyboards.SETTINGS)

        val SELECT_ICON = brief("设置头像", buildString {
            appendLine("使用方法：设置头像 id/名称")
            appendLine("👉设置头像 106103")
            appendLine("👉设置头像 高瀬 梨緒")
            appendLine(" ")
            append("⏬您可以点击下方按钮查看头像列表。")
        }).toMessage(Keyboards.collection("头像", "icons"))

        val SELECT_ICON_SUCCESS = brief("设置头像", "设置头像成功。")
            .toMessage(Keyboards.collection("头像", "icons"))

        val SELECT_PLATE = brief("设置牌子", buildString {
            appendLine("使用方法：设置牌子/设置姓名框 id/名称")
            appendLine("👉设置牌子 100501")
            appendLine("👉设置牌子 晓将")
            appendLine("👉设置姓名框 7sRefちほー2")
            appendLine(" ")
            append("⏬您可以点击下方按钮查看牌子列表。")
        }).toMessage(Keyboards.collection("牌子", "plates"))

        val SELECT_PLATE_SUCCESS = brief("设置牌子", "设置牌子成功。")
            .toMessage(Keyboards.collection("牌子", "plates"))

        val QUEUE_HELP = brief("排卡管理", "本功能可以提供机厅人数查询及更新功能，可以点击下方按钮进行操作：")
            .toMessage(Keyboards.QUEUE_HELP)
    }

    private val MusicDifficulty.emoji: String
        get() = when(this) {
            MusicDifficulty.Basic -> "\uD83D\uDFE9"
            MusicDifficulty.Advanced -> "\uD83D\uDFE8"
            MusicDifficulty.Expert -> "\uD83D\uDFE5"
            MusicDifficulty.Master -> "\uD83D\uDFEA"
            MusicDifficulty.ReMaster -> "⬜"
            MusicDifficulty.Utage -> "\uD83D\uDFEB"
        }

    private fun Keyboard.KeyboardRowBuilder.atButton(
        label: String,
        data: String,
        enter: Boolean = false,
        style: Int = RenderData.BLUE,
        id: String = "1"
    ) = button(
        id = id,
        renderData = RenderData(
            label = label, visitedLabel = label, style = style
        ),
        action = Action(
            type = Action.AT, data = data, permission = Permission(Permission.EVERYONE), enter = enter
        )
    )

    private fun Keyboard.KeyboardRowBuilder.linkButton(
        label: String,
        url: String,
        enter: Boolean = false,
        style: Int = RenderData.BLUE,
        id: String = "1"
    ) = button(
        id = id,
        renderData = RenderData(
            label = label, visitedLabel = label, style = style
        ),
        action = Action(
            type = Action.LINK, data = url, permission = Permission(Permission.EVERYONE), enter = enter
        )
    )

    private fun Keyboard.KeyboardRowBuilder.callbackButton(
        label: String,
        data: String,
        enter: Boolean = false,
        style: Int = RenderData.BLUE,
        id: String = "1"
    ) = button(
        id = id,
        renderData = RenderData(
            label = label, visitedLabel = label, style = style
        ),
        action = Action(
            type = Action.CALLBACK, data = data, permission = Permission(Permission.EVERYONE), enter = enter
        )
    )
}