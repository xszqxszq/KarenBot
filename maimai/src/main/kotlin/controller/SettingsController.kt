package xyz.xszq.bot.controller

import okio.IOException
import xyz.xszq.bot.Maimai
import xyz.xszq.bot.Maimai.Companion.textMode
import xyz.xszq.bot.chain
import xyz.xszq.bot.component.MaimaiQuery
import xyz.xszq.bot.component.MarkdownTemplates
import xyz.xszq.bot.database.MaimaiSettingsTable
import xyz.xszq.bot.database.QQBindTable
import xyz.xszq.bot.event.GroupMessageEvent
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.exception.UserNotFoundException
import xyz.xszq.bot.message.MessageChain
import xyz.xszq.bot.music.Item
import xyz.xszq.bot.music.MusicDifficulty
import xyz.xszq.bot.music.UserQueryParams
import xyz.xszq.bot.newLine
import xyz.xszq.bot.query.ComboQuery
import xyz.xszq.bot.query.ComboQuery.filterCharts
import xyz.xszq.bot.query.ComboQuery.filterMusics
import xyz.xszq.bot.query.ComboQuery.filterRecords
import xyz.xszq.bot.reply

@Suppress("unused")
class SettingsController(
    override val maimai: Maimai
): Controller(maimai) {
    override suspend fun setRoute() = maimai.route("/mai") {
        startsWith(listOf("bind", "绑定")) { args ->
            if ("水鱼" in args)
                return@startsWith
            val qq = args.toLongOrNull() ?: run {
                reply("使用方法：/bind qq号")
                return@startsWith
            }
            QQBindTable.update(this.sender.id, qq)
            val user = maimai.query.getQueryParams(this)
            if (noAPIBindFound(user)) {
                if (textMode())
                    reply(MaimaiQuery.NO_BACKEND_BINDINGS)
                else
                    reply(MarkdownTemplates.Templates.selectBackends(this.text.trim()))
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
            MaimaiSettingsTable[sender.id, "prober"] = "diving-fish"
            reply("设置查分器成功。")
        }
        startsWith(listOf("设置落雪", "落雪")) {
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
                if (textMode())
                    reply(buildString {
                        appendLine("使用方法：设置头像 <id/名称>")
                        appendLine("\t例：设置头像 106103")
                        appendLine("\t例：设置头像 高瀬 梨緒")
                        appendLine()
                        appendLine("\t收藏品列表：https://otmdb.cn/bot/maimai/icons")
                    }.trim().newLine())
                else
                    reply(MarkdownTemplates.Templates.SELECT_ICON)
                return@startsWith
            }
            MaimaiSettingsTable[sender.id, "icon"] = iconFile.id.toString()
            if (textMode())
                reply("设置头像成功。")
            else
                reply(MarkdownTemplates.Templates.SELECT_ICON_SUCCESS)
        }
        startsWith(listOf("设置牌子", "设置姓名框")) { plate ->
            val plateFile = maimai.maimaiData.plates.values.firstOrNull {
                it.id == plate.trim().toIntOrNull() ||
                        it.name == plate.trim() ||
                        Item.toSimplified(it.name) == plate.trim() ||
                        it.filename == plate.trim() ||
                        it.filename.substringBefore(".png") == plate.trim()
            } ?: run {
                if (textMode())
                    reply(buildString {
                        appendLine("使用方法：设置牌子/设置姓名框 id/名称")
                        appendLine("\t例：设置牌子 100501")
                        appendLine("\t例：设置牌子 晓将")
                        appendLine("\t例：设置姓名框 7sRefちほー2")
                        appendLine()
                        appendLine("\t牌子列表：https://otmdb.cn/bot/maimai/plates")
                    }.trim().newLine())
                else
                    reply(MarkdownTemplates.Templates.SELECT_PLATE)
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
            if (textMode())
                reply("设置牌子成功。")
            else
                reply(MarkdownTemplates.Templates.SELECT_PLATE_SUCCESS)
        }
        startsWith(listOf("设置mai", "设置b50")) {
            if (textMode())
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
                }.trim().newLine())
            else
                reply(MarkdownTemplates.Templates.SETTINGS)
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
