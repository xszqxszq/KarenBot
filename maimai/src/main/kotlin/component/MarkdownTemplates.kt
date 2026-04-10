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

    fun init(
        maimai: Maimai
    ) {
        jacketUrl = maimai.config.tokens["assets-jacket"] ?: throw Exception("assets-jacket missing")
    }
    object Keyboards {
        fun single(
            data: String,
            label: String,
            enter: Boolean = false
        ) = Keyboard.create {
            row {
                button(
                    id = "1",
                    action = Action(
                        type = Action.AT,
                        data = data,
                        permission = Permission(Permission.EVERYONE),
                        enter = enter
                    ),
                    renderData = RenderData(
                        label = label,
                        visitedLabel = label,
                        style = RenderData.BLUE
                    )
                )
            }
        }
        fun music(
            music: MusicInfo
        ) = Keyboard.create {
            row {
                music.charts.forEach { chart ->
                    val emoji = when(chart.difficulty) {
                        MusicDifficulty.Basic -> "\uD83D\uDFE9"
                        MusicDifficulty.Advanced -> "\uD83D\uDFE8"
                        MusicDifficulty.Expert -> "\uD83D\uDFE5"
                        MusicDifficulty.Master -> "\uD83D\uDFEA"
                        MusicDifficulty.ReMaster -> "⬜"
                        MusicDifficulty.Utage -> "\uD83D\uDFEB"
                    }
                    val display = if (music.charts.size < 5) "$emoji${chart.difficulty.brief}" else emoji
                    button(
                        id = "level",
                        action = Action(
                            type = Action.AT,
                            data = "${chart.difficulty.brief}${music.id}",
                            permission = Permission(Permission.EVERYONE),
                            enter = true
                        ),
                        renderData = RenderData(
                            label = display,
                            visitedLabel = display,
                            style = RenderData.BLUE
                        )
                    )
                }
            }
            row {
                button(
                    id = "1",
                    action = Action(
                        type = Action.AT,
                        data = "info ${music.id}",
                        permission = Permission(Permission.EVERYONE),
                        enter = true
                    ),
                    renderData = RenderData(
                        label = "\uD83D\uDCAF查成绩",
                        visitedLabel = "\uD83D\uDCAF查成绩",
                        style = RenderData.BLUE
                    )
                )
                button(
                    id = "2",
                    action = Action(
                        type = Action.AT,
                        data = "歌50 ${music.id}",
                        permission = Permission(Permission.EVERYONE),
                        enter = true
                    ),
                    renderData = RenderData(
                        label = "\uD83D\uDCDC歌50",
                        visitedLabel = "\uD83D\uDCDC歌50",
                        style = RenderData.BLUE
                    )
                )
            }
            row {
                button(
                    id = "3",
                    action = Action(
                        type = Action.AT,
                        data = "预览id${music.id}",
                        permission = Permission(Permission.EVERYONE),
                        enter = true
                    ),
                    renderData = RenderData(
                        label = "\uD83D\uDD0A试听一下",
                        visitedLabel = "\uD83D\uDD0A试听一下",
                        style = RenderData.GRAY
                    )
                )
                button(
                    id = "4",
                    action = Action(
                        type = Action.AT,
                        data = "添加别名 ${music.id}",
                        permission = Permission(Permission.EVERYONE)
                    ),
                    renderData = RenderData(
                        label = "➕添加别名",
                        visitedLabel = "➕添加别名",
                        style = RenderData.GRAY
                    )
                )
            }
        }
        fun oauth(
            authUrl: String
        ) = Keyboard.create {
            row {
                button(
                    id = "1",
                    action = Action(
                        type = Action.LINK,
                        data = authUrl,
                        permission = Permission(Permission.EVERYONE)
                    ),
                    renderData = RenderData(
                        label = "点我授权",
                        visitedLabel = "已授权",
                        style = RenderData.BLUE
                    )
                )
            }
        }
        fun collection(
            type: String,
            engType: String
        ) = Keyboard.create {
            row {
                button(
                    id = "1",
                    renderData = RenderData(
                        label = "选择$type",
                        visitedLabel = "选择$type",
                        style = RenderData.BLUE
                    ),
                    action = Action(
                        type = Action.LINK,
                        permission = Permission(Permission.EVERYONE),
                        data = "https://otmdb.cn/bot/maimai/$engType"
                    )
                )
                button(
                    id = "2",
                    renderData = RenderData(
                        label = "⚙ 设置$type",
                        visitedLabel = "⚙ 设置$type",
                        style = RenderData.BLUE
                    ),
                    action = Action(
                        type = Action.AT,
                        permission = Permission(Permission.EVERYONE),
                        data = "设置$type "
                    )
                )
            }
        }
        fun queue(
            now: String ?= null,
        ) = Keyboard.create {
            row {
                button(
                    id = "",
                    renderData = RenderData(
                        label = "查询人数",
                        visitedLabel = "查询人数",
                        style = RenderData.BLUE
                    ),
                    action = Action(
                        type = Action.AT,
                        permission = Permission(Permission.EVERYONE),
                        data = "/j",
                        enter = true
                    )
                )
                now ?.let {
                    button(
                        id = "",
                        renderData = RenderData(
                            label = "添加别名",
                            visitedLabel = "添加别名",
                            style = RenderData.BLUE
                        ),
                        action = Action(
                            type = Action.AT,
                            permission = Permission(Permission.EVERYONE),
                            data = "/排卡管理 添加别名 $now 这里填别名"
                        )
                    )
                }
                button(
                    id = "",
                    renderData = RenderData(
                        label = "更新人数",
                        visitedLabel = "更新人数",
                        style = RenderData.BLUE
                    ),
                    action = Action(
                        type = Action.AT,
                        permission = Permission(Permission.EVERYONE),
                        data = now ?: "\n"
                    )
                )
            }
        }
        fun image(
            command: String,
            nowPage: Int ?= null,
            totalPages: Int ?= null
        ) = Keyboard.create {
            row {
                button(
                    id = "1",
                    action = Action(
                        type = Action.AT,
                        data = command,
                        permission = Permission(Permission.EVERYONE)
                    ),
                    renderData = RenderData(
                        label = "\uD83D\uDCAF我也要查",
                        visitedLabel = "\uD83D\uDCAF我也要查",
                        style = RenderData.BLUE
                    )
                )
                button(
                    id = "2",
                    action = Action(
                        type = Action.LINK,
                        data = "https://otmdb.cn/bot/maimai/combo",
                        enter = true,
                        permission = Permission(Permission.EVERYONE)
                    ),
                    renderData = RenderData(
                        label = "随心配",
                        visitedLabel = "随心配",
                        style = RenderData.BLUE
                    )
                )
                button(
                    id = "3",
                    action = Action(
                        type = Action.AT,
                        data = "设置mai",
                        enter = true,
                        permission = Permission(Permission.EVERYONE)
                    ),
                    renderData = RenderData(
                        label = "\uD83C\uDFA8修改设置",
                        visitedLabel = "\uD83C\uDFA8修改设置",
                        style = RenderData.BLUE
                    )
                )
            }
            nowPage ?.let {
                if (totalPages == null)
                    return@let
                if (totalPages <= 1)
                    return@let
                row {
                    if (nowPage > 1)
                        button(
                            id = "4",
                            action = Action(
                                type = Action.AT,
                                data = "$command ${nowPage - 1}",
                                permission = Permission(Permission.EVERYONE),
                                enter = true
                            ),
                            renderData = RenderData(
                                label = "⬅\uFE0F上一页",
                                visitedLabel = "⬅\uFE0F上一页",
                                style = RenderData.BLUE
                            )
                        )
                    if (nowPage < totalPages)
                        button(
                            id = "5",
                            action = Action(
                                type = Action.AT,
                                data = "$command ${nowPage + 1}",
                                permission = Permission(Permission.EVERYONE),
                                enter = true
                            ),
                            renderData = RenderData(
                                label = "➡\uFE0F下一页",
                                visitedLabel = "➡\uFE0F下一页",
                                style = RenderData.BLUE
                            )
                        )
                }
            }
        }
        fun importData(
            backend: MaimaiAPI
        ) = Keyboard.create {
            when (backend) {
                is DivingFish ->
                    row {
                        button(
                            id = "1",
                            action = Action(
                                type = Action.LINK,
                                data = "https://otmdb.cn/jump/maimaidxprober_import",
                                permission = Permission(Permission.EVERYONE)
                            ),
                            renderData = RenderData(
                                label = "\uD83D\uDC1F水鱼(电脑端)",
                                visitedLabel = "\uD83D\uDC1F水鱼(电脑端)",
                                style = RenderData.BLUE
                            )
                        )
                    }
                is LXNS ->
                    row {
                        button(
                            id = "2",
                            action = Action(
                                type = Action.LINK,
                                data = "https://otmdb.cn/jump/lxnsprober_import",
                                permission = Permission(Permission.EVERYONE)
                            ),
                            renderData = RenderData(
                                label = "❄落雪(电脑/手机)",
                                visitedLabel = "❄落雪(电脑/手机)",
                                style = RenderData.BLUE
                            )
                        )
                    }
            }
            row {
                button(
                    id = "3",
                    action = Action(
                        type = Action.LINK,
                        data = "https://otmdb.cn/jump/maimai_prober_mobile",
                        permission = Permission(Permission.EVERYONE)
                    ),
                    renderData = RenderData(
                        label = "\uD83D\uDC07UsagiPass(iOS/安卓)",
                        visitedLabel = "\uD83D\uDC07UsagiPass(iOS/安卓)",
                        style = RenderData.BLUE
                    )
                )
            }
            if (backend is DivingFish)
                row {
                    button(
                        id = "4",
                        action = Action(
                            type = Action.LINK,
                            data = "https://www.bilibili.com/video/BV1Yg41167ie",
                            permission = Permission(Permission.EVERYONE)
                        ),
                        renderData = RenderData(
                            label = "\uD83E\uDD16Bakapiano(安卓)",
                            visitedLabel = "\uD83E\uDD16Bakapiano(安卓)",
                            style = RenderData.BLUE
                        )
                    )
                }
        }
        fun tryIt(
            command: String
        ) = single(command.trim() + " ", "⬇试一试")
        fun aliases(
            command: String
        ) = single(command.trim(), "添加别名")
        fun aliasVote(
            command: String
        ) = single(command.trim(), "点我投票", enter = true)

        fun selectPaged(
            button: String,
            keyword: String,
            nowPage: Int = 1,
            totalPages: Int = 1,
        ) = Keyboard.create {
            row {
                if (nowPage > 1)
                    button(
                        id = button,
                        action = Action(
                            type = Action.CALLBACK,
                            data = "$keyword\n${nowPage - 1}",
                            permission = Permission(Permission.EVERYONE),
                            enter = true
                        ),
                        renderData = RenderData(
                            label = "⬅\uFE0F上一页",
                            visitedLabel = "⬅\uFE0F上一页",
                            style = RenderData.BLUE
                        )
                    )
                if (nowPage < totalPages)
                    button(
                        id = button,
                        action = Action(
                            type = Action.CALLBACK,
                            data = "$keyword\n${nowPage + 1}",
                            permission = Permission(Permission.EVERYONE),
                            enter = true
                        ),
                        renderData = RenderData(
                            label = "➡\uFE0F下一页",
                            visitedLabel = "➡\uFE0F下一页",
                            style = RenderData.BLUE
                        )
                    )
            }
        }
        fun backends(
            message: String
        ) = Keyboard.create {
            row {
                button(
                    id = "1",
                    action = Action(
                        type = Action.LINK,
                        data = "https://otmdb.cn/jump/maimaidxprober",
                        permission = Permission(Permission.EVERYONE),
                    ),
                    renderData = RenderData(
                        label = "水鱼查分器",
                        visitedLabel = "水鱼查分器",
                        style = RenderData.BLUE
                    )
                )
                button(
                    id = "2",
                    action = Action(
                        type = Action.LINK,
                        data = "https://otmdb.cn/jump/lxnsprober",
                        permission = Permission(Permission.EVERYONE),
                    ),
                    renderData = RenderData(
                        label = "落雪查分器",
                        visitedLabel = "落雪查分器",
                        style = RenderData.BLUE
                    )
                )
            }
            row {
                button(
                    id = "3",
                    action = Action(
                        type = Action.AT,
                        data = message,
                        permission = Permission(Permission.EVERYONE),
                        enter = true
                    ),
                    renderData = RenderData(
                        label = "点我重试",
                        visitedLabel = "点我重试",
                        style = RenderData.FILLED_BLUE
                    )
                )
            }
        }
        
        val BIND_QQ = Keyboard.create {
            row {
                button(
                    id = "1",
                    action = Action(
                        type = Action.AT,
                        data = "/bind ",
                        permission = Permission(Permission.EVERYONE),
                    ),
                    renderData = RenderData(
                        label = "⬇点我输入",
                        visitedLabel = "⬇点我输入",
                        style = RenderData.BLUE
                    )
                )
            }
        }
        val BIND_DF = Keyboard.create {
            row {
                button(
                    id = "1",
                    action = Action(
                        type = Action.AT,
                        data = "/绑定水鱼 ",
                        permission = Permission(Permission.EVERYONE),
                    ),
                    renderData = RenderData(
                        label = "⬇点我输入",
                        visitedLabel = "⬇点我输入",
                        style = RenderData.BLUE
                    )
                )
            }
        }
        val HELP_PROXY = Keyboard.create {
            row {
                button(
                    id = "1",
                    action = Action(
                        type = Action.LINK,
                        data = "https://bot-docs.otmdb.cn/maimai/update",
                        permission = Permission(Permission.EVERYONE),
                    ),
                    renderData = RenderData(
                        label = "设置代理",
                        visitedLabel = "设置代理",
                        style = RenderData.BLUE
                    )
                )
            }
        }
        val UPDATE = Keyboard.create {
            row {
                button(
                    id = "1",
                    action = Action(
                        type = Action.AT,
                        data = "更新",
                        permission = Permission(Permission.EVERYONE),
                        enter = true
                    ),
                    renderData = RenderData(
                        label = "点击更新",
                        visitedLabel = "点击更新",
                        style = RenderData.BLUE
                    )
                )
            }
        }
        val USER_EULA = Keyboard.create {
            row {
                button(
                    id = "1",
                    action = Action(
                        type = Action.LINK,
                        data = "https://otmdb.cn/jump/maimaidxprober",
                        permission = Permission(Permission.EVERYONE),
                    ),
                    renderData = RenderData(
                        label = "前往查分器",
                        visitedLabel = "前往查分器",
                        style = RenderData.BLUE
                    )
                )
            }
        }
        val HELP = Keyboard.create {
            row {
                button(
                    id = "1",
                    action = Action(
                        type = Action.AT,
                        data = "牛奶歌是什么歌",
                        permission = Permission(Permission.EVERYONE),
                    ),
                    renderData = RenderData(
                        label = "\uD83D\uDD0E查歌",
                        visitedLabel = "\uD83D\uDD0E查歌",
                        style = RenderData.BLUE
                    )
                )
                button(
                    id = "1",
                    action = Action(
                        type = Action.AT,
                        data = "info 海底谭",
                        permission = Permission(Permission.EVERYONE),
                    ),
                    renderData = RenderData(
                        label = "\uD83D\uDCCB单曲成绩",
                        visitedLabel = "\uD83D\uDCCB单曲成绩",
                        style = RenderData.BLUE
                    )
                )
            }
            row {
                button(
                    id = "1",
                    action = Action(
                        type = Action.AT,
                        data = "b50",
                        permission = Permission(Permission.EVERYONE),
                    ),
                    renderData = RenderData(
                        label = "\uD83D\uDCAFBest50",
                        visitedLabel = "\uD83D\uDCAFBest50",
                        style = RenderData.BLUE
                    )
                )
                button(
                    id = "1",
                    action = Action(
                        type = Action.LINK,
                        data = "https://otmdb.cn/bot/maimai/combo",
                        permission = Permission(Permission.EVERYONE),
                    ),
                    renderData = RenderData(
                        label = "随心配50",
                        visitedLabel = "随心配50",
                        style = RenderData.BLUE
                    )
                )
            }
            row {
                button(
                    id = "1",
                    action = Action(
                        type = Action.AT,
                        data = "橙将完成表",
                        permission = Permission(Permission.EVERYONE),
                    ),
                    renderData = RenderData(
                        label = "⏳完成表",
                        visitedLabel = "⏳完成表",
                        style = RenderData.BLUE
                    )
                )
                button(
                    id = "1",
                    action = Action(
                        type = Action.AT,
                        data = "13分数列表",
                        permission = Permission(Permission.EVERYONE),
                    ),
                    renderData = RenderData(
                        label = "\uD83D\uDCD6分数列表",
                        visitedLabel = "\uD83D\uDCD6分数列表",
                        style = RenderData.BLUE
                    )
                )
            }
            row {
                button(
                    id = "1",
                    action = Action(
                        type = Action.AT,
                        data = "舞萌开字母",
                        permission = Permission(Permission.EVERYONE),
                    ),
                    renderData = RenderData(
                        label = "\uD83D\uDD79\uFE0F开字母",
                        visitedLabel = "\uD83D\uDD79\uFE0F开字母",
                        style = RenderData.BLUE
                    )
                )
                button(
                    id = "1",
                    action = Action(
                        type = Action.LINK,
                        data = "https://otmdb.cn/bot/maimai",
                        permission = Permission(Permission.EVERYONE),
                    ),
                    renderData = RenderData(
                        label = "更多功能...",
                        visitedLabel = "更多功能...",
                        style = RenderData.BLUE
                    )
                )
            }
        }
        val SETTINGS = Keyboard.create {
            row {
                button(
                    id = "1",
                    renderData = RenderData(
                        label = "\uD83D\uDC64设置头像",
                        visitedLabel = "\uD83D\uDC64设置头像",
                        style = RenderData.BLUE
                    ),
                    action = Action(
                        type = Action.AT,
                        permission = Permission(Permission.EVERYONE),
                        data = "设置头像",
                        enter = true
                    )
                )
                button(
                    id = "1",
                    renderData = RenderData(
                        label = "\uD83D\uDCF0设置牌子",
                        visitedLabel = "\uD83D\uDCF0设置牌子",
                        style = RenderData.BLUE
                    ),
                    action = Action(
                        type = Action.AT,
                        permission = Permission(Permission.EVERYONE),
                        data = "设置牌子",
                        enter = true
                    )
                )
            }
            row {
                button(
                    id = "1",
                    renderData = RenderData(
                        label = "\uD83D\uDC1F使用水鱼查分",
                        visitedLabel = "\uD83D\uDC1F使用水鱼查分",
                        style = RenderData.BLUE
                    ),
                    action = Action(
                        type = Action.AT,
                        permission = Permission(Permission.EVERYONE),
                        data = "设置查分器 水鱼",
                        enter = true
                    )
                )
                button(
                    id = "1",
                    renderData = RenderData(
                        label = "❄使用落雪查分",
                        visitedLabel = "❄使用落雪查分",
                        style = RenderData.BLUE
                    ),
                    action = Action(
                        type = Action.AT,
                        permission = Permission(Permission.EVERYONE),
                        data = "设置查分器 落雪",
                        enter = true
                    )
                )
            }
            row {
                button(
                    id = "1",
                    renderData = RenderData(
                        label = "\uD83D\uDD03自动选择查分器",
                        visitedLabel = "\uD83D\uDD03自动选择查分器",
                        style = RenderData.BLUE
                    ),
                    action = Action(
                        type = Action.AT,
                        permission = Permission(Permission.EVERYONE),
                        data = "设置查分器 自动",
                        enter = true
                    )
                )
            }
        }
        val QUEUE_INIT = Keyboard.create {
            row {
                button(
                    id = "",
                    renderData = RenderData(
                        label = "添加机厅",
                        visitedLabel = "添加机厅",
                        style = RenderData.BLUE
                    ),
                    action = Action(
                        type = Action.AT,
                        permission = Permission(Permission.EVERYONE),
                        data = "/排卡管理 添加机厅 机厅名称",
                    )
                )
            }
        }
        val QUEUE_HELP = Keyboard.create {
            row {
                button(
                    id = "",
                    renderData = RenderData(
                        label = "查询人数",
                        visitedLabel = "查询人数",
                        style = RenderData.BLUE
                    ),
                    action = Action(
                        type = Action.AT,
                        permission = Permission(Permission.EVERYONE),
                        data = "几",
                        enter = true
                    )
                )
                button(
                    id = "",
                    renderData = RenderData(
                        label = "添加机厅",
                        visitedLabel = "添加机厅",
                        style = RenderData.BLUE
                    ),
                    action = Action(
                        type = Action.AT,
                        permission = Permission(Permission.EVERYONE),
                        data = "/排卡管理 添加机厅 机厅名称"
                    )
                )
                button(
                    id = "",
                    renderData = RenderData(
                        label = "删除机厅",
                        visitedLabel = "删除机厅",
                        style = RenderData.BLUE
                    ),
                    action = Action(
                        type = Action.AT,
                        permission = Permission(Permission.EVERYONE),
                        data = "/排卡管理 删除机厅 机厅名称"
                    )
                )
            }
            row {
                button(
                    id = "",
                    renderData = RenderData(
                        label = "查看别名",
                        visitedLabel = "查看别名",
                        style = RenderData.BLUE
                    ),
                    action = Action(
                        type = Action.AT,
                        permission = Permission(Permission.EVERYONE),
                        data = "/排卡管理 查看别名 机厅名称"
                    )
                )
                button(
                    id = "",
                    renderData = RenderData(
                        label = "添加别名",
                        visitedLabel = "添加别名",
                        style = RenderData.BLUE
                    ),
                    action = Action(
                        type = Action.AT,
                        permission = Permission(Permission.EVERYONE),
                        data = "/排卡管理 添加别名 机厅名称 别名名称"
                    )
                )
                button(
                    id = "",
                    renderData = RenderData(
                        label = "删除别名",
                        visitedLabel = "删除别名",
                        style = RenderData.BLUE
                    ),
                    action = Action(
                        type = Action.AT,
                        permission = Permission(Permission.EVERYONE),
                        data = "/排卡管理 删除别名 机厅名称 别名名称"
                    )
                )
            }
        }
        val QUEUE_UPDATE = Keyboard.create {
            row {
                button(
                    id = "",
                    renderData = RenderData(
                        label = "更新人数",
                        visitedLabel = "更新人数",
                        style = RenderData.BLUE
                    ),
                    action = Action(
                        type = Action.AT,
                        permission = Permission(Permission.EVERYONE),
                        data = "\n"
                    )
                )
            }
        }
        val GUESS = Keyboard.create {
            row {
                button(
                    id = "1",
                    action = Action(
                        type = Action.AT,
                        data = " ",
                        permission = Permission(Permission.EVERYONE),
                    ),
                    renderData = RenderData(
                        label = "⬇输入答案",
                        visitedLabel = "⬇输入答案",
                        style = RenderData.FILLED_BLUE
                    )
                )
            }
            row {
                button(
                    id = "2",
                    action = Action(
                        type = Action.AT,
                        data = "不玩了",
                        permission = Permission(Permission.EVERYONE),
                        enter = true
                    ),
                    renderData = RenderData(
                        label = "不玩了",
                        visitedLabel = "不玩了",
                        style = RenderData.RED
                    )
                )
            }
        }
        val GUESS_AGAIN = Keyboard.create {
            row {
                button(
                    id = "1",
                    action = Action(
                        type = Action.AT,
                        data = "猜歌",
                        permission = Permission(Permission.EVERYONE),
                        enter = true
                    ),
                    renderData = RenderData(
                        label = "\uD83D\uDD79\uFE0F再玩一把",
                        visitedLabel = "\uD83D\uDD79\uFE0F再玩一把",
                        style = RenderData.BLUE
                    )
                )
            }
        }
        val GUESS_OPEN_AGAIN = Keyboard.create {
            row {
                button(
                    id = "1",
                    action = Action(
                        type = Action.AT,
                        data = "舞萌开字母",
                        permission = Permission(Permission.EVERYONE),
                        enter = true
                    ),
                    renderData = RenderData(
                        label = "\uD83D\uDD79\uFE0F再玩一把",
                        visitedLabel = "\uD83D\uDD79\uFE0F再玩一把",
                        style = RenderData.BLUE
                    )
                )
            }
        }
    }

    object Templates {
        fun music(
            music: MusicInfo,
            cover: String
        ) = Markdown(MarkdownData(buildString {
            appendLine("![img#190px #190px]($cover)")
            appendLine("**${music.id}. ${music.name}**")

            appendLine("**曲师:** ${music.artist}")
            appendLine("**分类:** ${music.genre.genreName}")
            appendLine("**版本:** ${music.version.name}")
            appendLine("**BPM:** ${music.bpm}")
            appendLine(
                "**定数:** ${
                    music.charts.joinToString("/") {
                        it.levelValue.toString()
                    }
                }"
            )
            appendLine(
                "**拟合定数:** ${
                    music.charts.joinToString("/") {
                        if (it.fitLevelValue == 0.0)
                            "-"
                        else
                            it.fitLevelValue.toStringDecimal(1)
                    }
                }"
            )
            appendLine("**谱师:** ${music.charts.joinToString("/") { it.notesDesigner }.ifBlank { "-" }}")
        }), Keyboards.music(music))
        fun chart(
            chart: ChartInfo,
            cover: String
        ) = Markdown(MarkdownData(buildString {
            appendLine("![img#190px #190px]($cover)")
            appendLine("**${chart.difficulty.names.last()}${chart.music.id}. ${chart.music.name}**")
            appendLine("**等级:** ${chart.level} (${chart.levelValue})")
            appendLine("**TAP:** ${chart.notes.tap}")
            appendLine("**HOLD:** ${chart.notes.hold}")
            appendLine("**SLIDE:** ${chart.notes.slide}")
            appendLine("**BREAK:** ${chart.notes.`break`}")
            if (chart.notes.touch != 0)
                appendLine("**TOUCH:** ${chart.notes.touch}")
            appendLine("**总物量:** ${chart.notes.total}")
            appendLine("**总DX分:** ${chart.maxDeluxeScore}")
            appendLine("**谱师:** ${chart.notesDesigner}")
            if (chart.fitLevelValue != 0.0)
                appendLine("**拟合定数:** ${chart.fitLevelValue.toStringDecimal(1)}")
        }))
        fun oauth(
            authUrl: String
        ) = Markdown(MarkdownData(buildString {
            appendLine("**请求授权**")
            appendLine()
            appendLine("使用该功能时，需要您授权BOT访问您在落雪查分器的全部成绩信息，请点击下方登录并授权：")
        }), Keyboards.oauth(authUrl))
        fun queue(
            title: String,
            content: String,
            now: String ?= null,
        ) = Markdown(MarkdownData(buildString {
            appendLine("**$title**")
            appendLine()
            appendLine(content)
        }), Keyboards.queue(now))
        fun queueInit(
            title: String,
            content: String
        ) = Markdown(MarkdownData(buildString {
            appendLine("**$title**")
            appendLine()
            appendLine(content)
        }), Keyboards.QUEUE_INIT)
        fun queueUpdate(
            info: String
        ) = Markdown(MarkdownData(buildString {
            appendLine("**机厅排卡人数：**")
            appendLine()
            appendLine(info.split("\n").joinToString("\n") { "> $it" })
            appendLine("> ")
            appendLine("> 更新数据请使用“机厅名+数量”。\n\t例：某某机厅3\n\t例：机厅+1\n\t例：jt-2")
        }), Keyboards.QUEUE_UPDATE)
        fun image(
            url: String,
            size: Pair<Int, Int>,
            command: String,
            description: String?,
            nowPage: Int ?= null,
            totalPages: Int ?= null
        ) = Markdown(MarkdownData(buildString {
            appendLine("**查询结果**")
            appendLine()
            appendLine("![img #${size.first}px #${size.second}px]($url)")
            appendLine()
            appendLine(description ?: "")
        }), Keyboards.image(command, nowPage, totalPages))
        fun brief(
            title: String,
            content: String,
        ) = MarkdownData(buildString {
            appendLine("**$title**")
            appendLine()
            appendLine(content)
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
            appendLine(description)
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
            displayName: String ?= null,
            nowPage: Int = 1,
            totalPages: Int = 1,
        ): Markdown {
            val rows = result.map { music ->
                val url = "$jacketUrl/${music.resourceId}.jpg"
                buildString {
                    append("![preview #25px #25px]($url)")
                    append(' ')
                    append("<qqbot-cmd-input text=\"")
                    append("${displayName ?: type} ${difficulty?.brief ?: ""}id${music.id}".trim().encodeURLParameter())
                    append("\" show=\"")
                    append("${music.id}. ${music.name}".encodeURLParameter())
                    append("\" reference=\"false\"/>")
                }
            }.take(20)

            val data = MarkdownData(buildString {
                appendLine("| $title |")
                appendLine("| --- |")
                rows.forEach { row ->
                    append("|")
                    append(row)
                    append("|")
                    appendLine()
                }
            })
            val keyboard = when {
                totalPages == 1 -> null
                else -> Keyboards.selectPaged(type, keyword, nowPage, totalPages)
            }
            return Markdown(data, keyboard)
        }
        fun selectBackends(
            message: String
        ) = Markdown(
            brief("舞萌DX", "您还未在查分器上绑定QQ号。请选择一个查分器来绑定您的QQ号："),
            Keyboards.backends(message)
        )

        val BIND_QQ = brief("舞萌DX", "为了继续后续查询，请输入您的QQ号来绑定：")
            .toMessage(Keyboards.BIND_QQ)
        val USER_EULA = brief("舞萌DX", "请前往查分器同意用户协议再进行查询：")
            .toMessage(Keyboards.USER_EULA)
        val HELP = brief("舞萌DX", buildString {
            appendLine("这是一个查询舞萌DX成绩及相关信息的功能。")
            appendLine("支持以下功能指令：")
        }.trim()).toMessage(Keyboards.HELP)
        val SETTINGS = brief("功能设置", "支持以下设定：")
            .toMessage(Keyboards.SETTINGS)
        val SELECT_ICON = brief("设置头像",
            buildString {
                appendLine("使用方法：设置头像 id/名称")
                appendLine("\uD83D\uDC49\uFE0F设置头像 106103")
                appendLine("\uD83D\uDC49\uFE0F设置头像 高瀬 梨緒")
                appendLine(" ")
                appendLine("⏬您可以点击下方按钮查看头像列表。")
            }.trim()
        ).toMessage(Keyboards.collection("头像", "icons"))
        val SELECT_ICON_SUCCESS = brief("设置头像", "设置头像成功。")
            .toMessage(Keyboards.collection("头像", "icons"))
        val SELECT_PLATE = brief("设置牌子",
            buildString {
                appendLine("使用方法：设置牌子/设置姓名框 id/名称")
                appendLine("\uD83D\uDC49\uFE0F设置牌子 100501")
                appendLine("\uD83D\uDC49\uFE0F设置牌子 晓将")
                appendLine("\uD83D\uDC49\uFE0F设置姓名框 7sRefちほー2")
                appendLine(" ")
                appendLine("⏬您可以点击下方按钮查看牌子列表。")
            }.trim()
        ).toMessage(Keyboards.collection("牌子", "plates"))
        val SELECT_PLATE_SUCCESS = brief("设置牌子", "设置牌子成功。")
            .toMessage(Keyboards.collection("牌子", "plates"))
        val QUEUE_HELP = brief("排卡管理", "本功能可以提供机厅人数查询及更新功能，可以点击下方按钮进行操作：")
            .toMessage(Keyboards.QUEUE_HELP)
    }
}