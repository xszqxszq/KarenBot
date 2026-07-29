package xyz.xszq.bot.maimai.controller

import xyz.xszq.bot.Maimai
import xyz.xszq.bot.chain
import xyz.xszq.bot.event.GroupMessageEvent
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.maimai.database.MaimaiSettingsTable
import xyz.xszq.bot.maimai.database.QQBindTable
import xyz.xszq.bot.maimai.music.Item
import xyz.xszq.bot.maimai.music.MusicDifficulty
import xyz.xszq.bot.maimai.music.UserQueryParams
import xyz.xszq.bot.maimai.query.ComboQuery
import xyz.xszq.bot.maimai.query.ComboQuery.filterCharts
import xyz.xszq.bot.maimai.query.ComboQuery.filterMusics
import xyz.xszq.bot.maimai.query.ComboQuery.filterRecords
import xyz.xszq.bot.message.MessageChain
import xyz.xszq.bot.newLine
import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.reply

@Suppress("unused")
class SettingsController(
    override val maimai: Maimai
): Controller(maimai) {
    private suspend fun route() = rhythm {
        startsWith(listOf("bind", "绑定")) { args ->
            val qq = args.toLongOrNull()
            when {
                "水鱼" in args -> return@startsWith
                args.startsWith("SGWCMAID") -> {
                    reply("输入错误，使用方法：/bind qq号\n请不要随意泄露自己的账号二维码") {
                        brief("可怜BOT", "输入错误，请不要随意泄露自己的账号二维码\n\n请点击下方按钮输入您的QQ号：")
                        keyboard {
                            row {
                                at("⬇点我输入", "/bind ")
                            }
                        }
                    }
                    return@startsWith
                }
                qq == null -> {
                    reply("使用方法：/bind qq号") {
                        brief("可怜BOT", "请点击下方按钮输入您的QQ号：")
                        keyboard {
                            row {
                                at("⬇点我输入", "/bind ")
                            }
                        }
                    }
                    return@startsWith
                }
            }
            QQBindTable.update(this.sender.id, qq)
            val user = maimai.query.getQueryParams(this)
            if (noAPIBindFound(user)) {
                messageUserNeedBind()
            } else {
                reply("绑定成功。")
                maimai.messageToReplay[sender.id] ?.let { text ->
                    replayMessage(text.chain())
                    maimai.messageToReplay.remove(sender.id)
                }
            }
        }
        startsWith("设置查分器") { name ->
            when {
                "水鱼" in name -> {
                    MaimaiSettingsTable[sender.id, "prober"] = "diving-fish"
                }
                "落雪" in name -> {
                    MaimaiSettingsTable[sender.id, "prober"] = "lxns"
                }
                "自动" in name -> {
                    MaimaiSettingsTable[sender.id, "prober"] = ""
                }
                else -> {
                    reply(buildString {
                        appendLine("使用方法：设置查分器 <查分器名称>")
                        appendLine("\t例：设置查分器 水鱼")
                        appendLine("\t例：设置查分器 落雪")
                        appendLine("\t例：设置查分器 自动")
                    }.trim().newLine())
                    return@startsWith
                }
            }
            reply("设置查分器成功。")
        }
        startsWith(listOf("设置水鱼", "水鱼")) {
            if (!text.startsWith("设置") && text.trim() != "水鱼")
                return@startsWith
            MaimaiSettingsTable[sender.id, "prober"] = "diving-fish"
            reply("设置查分器成功。")
        }
        startsWith(listOf("设置落雪", "落雪")) {
            if (!text.startsWith("设置") && text.trim() != "落雪")
                return@startsWith
            MaimaiSettingsTable[sender.id, "prober"] = "lxns"
            reply("设置查分器成功。")
        }
        startsWith("兼容模式") { arg ->
            when {
                arg.trim() in listOf("取消", "关闭", "禁用") -> {
                    MaimaiSettingsTable[sender.id, "text-mode"] = "0"
                    reply("兼容模式禁用成功。")
                }
                else -> {
                    MaimaiSettingsTable[sender.id, "text-mode"] = "1"
                    reply("兼容模式启用成功，如需关闭请@机器人并发送“兼容模式 关闭”。")
                }
            }
        }
        startsWith(listOf("取消兼容模式", "关闭兼容模式", "禁用兼容模式")) {
            MaimaiSettingsTable[sender.id, "text-mode"] = "0"
            reply("兼容模式禁用成功。")
        }
        startsWith(listOf("打开兼容模式", "启用兼容模式")) {
            MaimaiSettingsTable[sender.id, "text-mode"] = "1"
            reply("兼容模式启用成功，如需关闭请@机器人并发送“兼容模式 关闭”。")
        }
        startsWith("设置头像") { icon ->
            val iconFile = maimai.maimaiData.icons.values.firstOrNull {
                it.id == icon.trim().toIntOrNull() ||
                        it.name == icon.trim() ||
                        it.filename == icon.trim() ||
                        it.filename.substringBefore(".png") == icon.trim()
            } ?: run {
                reply(buildString {
                    appendLine("使用方法：设置头像 <id/名称>")
                    appendLine("\t例：设置头像 106103")
                    appendLine("\t例：设置头像 高瀬 梨緒")
                    appendLine()
                    appendLine("\t收藏品列表：https://otmdb.cn/bot/maimai/icons")
                }.trim().newLine()) {
                    brief("设置头像", buildString {
                        appendLine("使用方法：设置头像 id/名称")
                        appendLine("👉设置头像 106103")
                        appendLine("👉设置头像 高瀬 梨緒")
                        appendLine(" ")
                        append("⏬您可以点击下方按钮查看头像列表。")
                    })
                    keyboard {
                        row {
                            link("选择头像", "https://otmdb.cn/bot/maimai/icons")
                            at("⚙ 设置头像", "/mai 设置头像 ")
                        }
                    }
                }
                return@startsWith
            }
            MaimaiSettingsTable[sender.id, "icon"] = iconFile.id.toString()
            reply("设置头像成功。") {
                brief("设置头像", "设置头像成功。")
                keyboard {
                    row {
                        link("选择头像", "https://otmdb.cn/bot/maimai/icons")
                        at("⚙ 设置头像", "/mai 设置头像 ")
                    }
                }
            }
        }
        startsWith(listOf("设置牌子", "设置姓名框")) { plate ->
            val plateFile = maimai.maimaiData.plates.values.firstOrNull {
                it.id == plate.trim().toIntOrNull() ||
                        it.name == plate.trim() ||
                        Item.toSimplified(it.name) == plate.trim() ||
                        it.filename == plate.trim() ||
                        it.filename.substringBefore(".png") == plate.trim()
            } ?: run {
                reply(buildString {
                    appendLine("使用方法：设置牌子/设置姓名框 id/名称")
                    appendLine("\t例：设置牌子 100501")
                    appendLine("\t例：设置牌子 晓将")
                    appendLine("\t例：设置姓名框 7sRefちほー2")
                    appendLine()
                    appendLine("\t牌子列表：https://otmdb.cn/bot/maimai/plates")
                }.trim().newLine()) {
                    brief("设置牌子", buildString {
                        appendLine("使用方法：设置牌子/设置姓名框 id/名称")
                        appendLine("👉设置牌子 100501")
                        appendLine("👉设置牌子 晓将")
                        appendLine("👉设置姓名框 7sRefちほー2")
                        appendLine(" ")
                        append("⏬您可以点击下方按钮查看牌子列表。")
                    })
                    keyboard {
                        row {
                            link("选择牌子", "https://otmdb.cn/bot/maimai/plates")
                            at("⚙ 设置牌子", "/mai 设置牌子 ")
                        }
                    }
                }
                return@startsWith
            }
            if (plateFile.genre == "実績" && plateFile.requires.isNotEmpty()) {
                val user = maimai.query.getQueryParams(this)
                val filters = ComboQuery.filters(Item.toSimplified(plateFile.name))
                val musics = filters.filterMusics(maimai.musics())
                val charts = filters.filterCharts(maimai.musics()).filter {
                    it.difficulty.value >= MusicDifficulty.Master.value
                }
                val (response, _) = maimai.query.records(user, musics)
                val records = filters.filterRecords(
                    response.records.filter { it.chart.difficulty.value >= MusicDifficulty.Master.value },
                    true
                ) ?: return@startsWith
                if (records.size < charts.size) {
                    reply("您未达成该牌子的获得条件。")
                    return@startsWith
                }
            }
            MaimaiSettingsTable[sender.id, "plate"] = plateFile.id.toString()
            reply("设置牌子成功。") {
                brief("设置牌子", "设置牌子成功。")
                keyboard {
                    row {
                        link("选择牌子", "https://otmdb.cn/bot/maimai/plates")
                        at("⚙ 设置牌子", "/mai 设置牌子 ")
                    }
                }
            }
        }
        startsWith(listOf("设置mai", "设置b50")) {
            reply(buildString {
                appendLine("支持以下设置：")
                appendLine("→设置头像 头像ID/名称")
                appendLine("\t例：设置头像 106103")
                appendLine("\t例：设置头像 高瀬 梨緒")
                appendLine("→设置牌子 牌子ID/名称")
                appendLine("\t例：设置牌子 100501")
                appendLine("\t例：设置牌子 晓将")
                appendLine("→设置查分器 查分器名称")
                appendLine("\t例：设置查分器 水鱼")
                appendLine("\t例：设置查分器 落雪")
            }.trim().newLine()) {
                brief("功能设置", "支持以下设定：")
                keyboard {
                    row {
                        at("👤设置头像", "/mai 设置头像", enter = true)
                        at("📰设置牌子", "/mai 设置牌子", enter = true)
                    }
                    row {
                        at("🐟使用水鱼查分", "设置查分器 水鱼", enter = true)
                        at("❄使用落雪查分", "设置查分器 落雪", enter = true)
                    }
                    row {
                        at("🔄自动选择查分器", "设置查分器 自动", enter = true)
                    }
                }
            }
        }
        channel<MessageEvent>("lxns-oa") { target ->
            target.requestOA()
        }
    }

    override suspend fun setRoute() {
        route()
        maimai.route("/mai", true) {
            startsWith(listOf("默认", "设为默认")) {
                MaimaiSettingsTable.setDefaultGame(sender.id, "maimai")
                reply("设置成功，在不带“/mai”“/chu”命令前缀时，将默认选择使用舞萌DX的相关功能")
            }
        }
    }

    private fun collection(type: String, id: String) = Keyboard.create {
        row {
            link("选择$type", "https://otmdb.cn/bot/maimai/$id", id = "1")
            at("⚙ 设置$type", "/mai 设置$type ", id = "2")
        }
    }

    private suspend fun noAPIBindFound(
        user: UserQueryParams
    ): Boolean {
        maimai.query.listBackends(user).forEach { backend ->
            runCatching {
                backend.getPlayerRating(user)
            }.onSuccess { response ->
                return false
            }
        }
        return true
    }

    private suspend fun MessageEvent.replayMessage(
        message: MessageChain,
    ) {
        bot.pluginLoader.subscribes.handle(when (this) {
            is GroupMessageEvent -> GroupMessageEvent(
                bot = bot,
                eventId = eventId,
                id = id,
                message = message,
                sender = sender,
                group = group,
                seq = seq + 1
            )
            else -> MessageEvent(
                bot = bot,
                eventId = eventId,
                id = id,
                message = message,
                sender = sender,
                seq = seq + 1
            )
        })
    }

}
