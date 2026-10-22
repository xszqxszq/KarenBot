package xyz.xszq.bot.maimai.controller

import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.event.ReplyAble
import xyz.xszq.bot.maimai.Maimai
import xyz.xszq.bot.maimai.api.DivingFish
import xyz.xszq.bot.maimai.api.LXNS
import xyz.xszq.bot.maimai.component.MarkdownTemplates.href
import xyz.xszq.bot.maimai.component.MarkdownTemplates.judgeUrl
import xyz.xszq.bot.maimai.component.MarkdownTemplates.outlineUrl
import xyz.xszq.bot.maimai.database.MaimaiPreviewSettingsTable
import xyz.xszq.bot.maimai.database.ProberBindTable
import xyz.xszq.bot.maimai.database.RhythmGameSettingsTable
import xyz.xszq.bot.maimai.music.Item
import xyz.xszq.bot.maimai.music.MusicDifficulty
import xyz.xszq.bot.maimai.music.UserQueryParams
import xyz.xszq.bot.maimai.query.ComboQuery
import xyz.xszq.bot.maimai.query.ComboQuery.filterCharts
import xyz.xszq.bot.maimai.query.ComboQuery.filterMusics
import xyz.xszq.bot.maimai.query.ComboQuery.filterRecords
import xyz.xszq.bot.newLine
import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.payload.markdown.RenderData
import xyz.xszq.bot.reply

/**
 * 个人自定义设置功能
 */
@Suppress("unused")
class SettingsController(
    override val maimai: Maimai
): Controller(maimai) {
    private val judgeStyleNames = listOf(
        "1-A", "1-B", "1-C", "1-D", "1-E",
        "2-A", "2-B", "2-C", "2-D", "2-E", "2-F",
        "3-B", "3-C", "3-D", "4-E"
    )
    private val outlineStyleNames = listOf(
        "无", "点", "简洁", "感应区", "maimai", "GreeN", "ORANGE", "PiNK",
        "MURASAKi", "MiLK", "FiNALE", "DX", "Splash", "UNiVERSE", "FESTiVAL", "BUDDiES", "PRiSM"
    )
    private val outlineStyleEnumNames = listOf(
        "Hide", "Dot", "Simple", "Sensor", "maimai", "GreeN", "ORANGE", "PiNK",
        "MURASAKi", "MiLK", "FiNALE", "DX", "Splash", "UNiVERSE", "FESTiVAL", "BUDDiES", "PRiSM"
    )
    private val tapStyleNames = listOf("默认", "经典", "实心", "砖块")
    private val holdStyleNames = listOf("默认", "经典")
    private val slideStyleNames = listOf("默认", "经典")

    private suspend fun route() = rhythm {
        startsWith(listOf("unbind", "解绑")) { _ ->
            ProberBindTable.delete(sender.id)
            (maimai.backend("diving-fish") as DivingFish).clearTokenCache(sender.id)
            (maimai.backend("lxns") as LXNS).clearTokenCache(sender.id)
            reply("解绑成功。")
        }
        startsWith(listOf("bind", "绑定")) { _ ->
            messageUserNeedBind(false, fromBind = true)
        }
        startsWith("设置查分器") { name ->
            when {
                "水鱼" in name -> {
                    RhythmGameSettingsTable[sender.id, "prober"] = "diving-fish"
                }
                "落雪" in name -> {
                    RhythmGameSettingsTable[sender.id, "prober"] = "lxns"
                }
                "自动" in name -> {
                    RhythmGameSettingsTable[sender.id, "prober"] = ""
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
            RhythmGameSettingsTable[sender.id, "prober"] = "diving-fish"
            reply("设置查分器成功。")
        }
        startsWith(listOf("设置落雪", "落雪")) {
            if (!text.startsWith("设置") && text.trim() != "落雪")
                return@startsWith
            RhythmGameSettingsTable[sender.id, "prober"] = "lxns"
            reply("设置查分器成功。")
        }
        startsWith("设置谱面确认") { arg ->
            maimaiPreviewSettings(sender.id, arg.trim())
        }
        button("maimai/preview-menu") {
            maimaiPreviewSubmenu(sender.id, data)
        }
        button("maimai/preview-set") {
            maimaiPreviewApply(sender.id, data)
        }
        startsWith("兼容模式") { arg ->
            when {
                arg.trim() in listOf("取消", "关闭", "禁用") -> {
                    RhythmGameSettingsTable[sender.id, "text-mode"] = "0"
                    reply("兼容模式禁用成功。")
                }
                else -> {
                    RhythmGameSettingsTable[sender.id, "text-mode"] = "1"
                    reply("兼容模式启用成功，如需关闭请@机器人并发送“兼容模式 关闭”。")
                }
            }
        }
        startsWith(listOf("取消兼容模式", "关闭兼容模式", "禁用兼容模式")) {
            RhythmGameSettingsTable[sender.id, "text-mode"] = "0"
            reply("兼容模式禁用成功。")
        }
        startsWith(listOf("打开兼容模式", "启用兼容模式")) {
            RhythmGameSettingsTable[sender.id, "text-mode"] = "1"
            reply("兼容模式启用成功，如需关闭请@机器人并发送“兼容模式 关闭”。")
        }
        startsWith("设置头像") { icon ->
            val iconFile = maimai.maimaiData.icons.values.firstOrNull {
                it.id == icon.trim().toIntOrNull() ||
                        it.name == icon.trim() ||
                        it.filename == icon.trim() ||
                        it.filename.substringBeforeLast(".") == icon.trim()
            } ?: run {
                reply(buildString {
                    appendLine("使用方法：设置头像 <id/名称>")
                    appendLine("\t例：设置头像 106103")
                    appendLine("\t例：设置头像 高瀬 梨緒")
                    appendLine()
                    appendLine("\t收藏品列表：https://docs.karenbot.cn/maimai/icons")
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
                            link("选择头像", "https://docs.karenbot.cn/maimai/icons")
                            at("⚙ 设置头像", "/mai 设置头像 ")
                        }
                    }
                }
                return@startsWith
            }
            RhythmGameSettingsTable[sender.id, RhythmGameSettingsTable.ICON_KEY] =
                iconFile.id.toString()
            reply("设置头像成功。") {
                brief("设置头像", "设置头像成功。")
                keyboard {
                    row {
                        link("选择头像", "https://docs.karenbot.cn/maimai/icons")
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
                        it.filename.substringBeforeLast(".") == plate.trim()
            } ?: run {
                reply(buildString {
                    appendLine("使用方法：设置牌子/设置姓名框 id/名称")
                    appendLine("\t例：设置牌子 100501")
                    appendLine("\t例：设置牌子 晓将")
                    appendLine("\t例：设置姓名框 7sRefちほー2")
                    appendLine()
                    appendLine("\t牌子列表：https://docs.karenbot.cn/maimai/plates")
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
                            link("选择牌子", "https://docs.karenbot.cn/maimai/plates")
                            at("⚙ 设置牌子", "/mai 设置牌子 ")
                        }
                    }
                }
                return@startsWith
            }
            if (plateFile.genre == "実績" && plateFile.requires.isNotEmpty()) {
                var user: UserQueryParams? = null
                runCatching {
                    user = maimai.query.getQueryParams(this)
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
                }.onFailure { e ->
                    handleError(this, e, user)
                    return@startsWith
                }
            }
            RhythmGameSettingsTable[sender.id, RhythmGameSettingsTable.PLATE_KEY] =
                plateFile.id.toString()
            reply("设置牌子成功。") {
                brief("设置牌子", "设置牌子成功。")
                keyboard {
                    row {
                        link("选择牌子", "https://docs.karenbot.cn/maimai/plates")
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
        channel<MessageEvent>("rhythm-game-bind") { target ->
            target.messageUserNeedBind(false)
        }
    }

    override suspend fun setRoute() {
        MaimaiPreviewSettingsTable.load()
        route()
        maimai.route("/mai", true) {
            startsWith(listOf("默认", "设为默认")) {
                RhythmGameSettingsTable.setDefaultGame(sender.id, "maimai")
                reply("设置成功，在不带“/mai”“/chu”命令前缀时，将默认选择使用舞萌DX的相关功能")
            }
        }
    }

    private suspend fun ReplyAble.maimaiPreviewSettings(openId: String, argument: String) {
        val current = MaimaiPreviewSettingsTable.settings(openId)
        if (argument.isNotEmpty()) {
            val updated = parsePreviewArgument(argument, current)
            if (updated == null) {
                reply("无法识别该参数，发送「设置谱面确认」查看当前数值。")
                return
            }
            MaimaiPreviewSettingsTable.put(openId, updated)
            reply("设置成功。")
            return
        }
        reply(buildString {
            appendLine("请使用“/兼容模式 禁用”关闭兼容模式来修改设置。")
        }.trim().newLine()) {
            brief("谱面预览设置", "请点击按钮来修改设置：")
            keyboard {
                row {
                    at("音符流速 (${current.noteSpeed})", "/mai 设置谱面确认 流速 ", enter = false)
                    at("Touch 流速 (${current.touchSpeed})", "/mai 设置谱面确认 Touch ", enter = false)
                }
                row {
                    at("星星时间 (${formatSlideSpeed(current.slideSpeed)})", "/mai 设置谱面确认 星星时间 ", enter = false)
                    callback("星星头 (${starTypeName(current.starType)})", "menu:star", "maimai/preview-menu")
                }
                row {
                    callback("判定样式 (${judgeStyleName(current.judgeStyle)})", "menu:judge", "maimai/preview-menu")
                    callback("音符样式 (${noteStyleName(current.tapStyle)})", "menu:note", "maimai/preview-menu")
                }
                row {
                    callback("边框样式 (${outlineStyleName(current.outlineStyle)})", "menu:outline", "maimai/preview-menu")
                    at("背景亮度 (${current.brightness}%)", "/mai 设置谱面确认 亮度 ", enter = false)
                }
            }
        }
    }

    private suspend fun ReplyAble.maimaiPreviewSubmenu(openId: String, menu: String) {
        val current = MaimaiPreviewSettingsTable.settings(openId)
        when (menu) {
            "menu:slide" -> reply("星星时间") {
                brief("星星时间", "当前 ${formatSlideSpeed(current.slideSpeed)}")
                keyboard {
                    for (start in 0..20 step 5) {
                        row {
                            for (index in start until minOf(start + 5, 21)) {
                                callback(
                                    formatSlideSpeed(index),
                                    "set:slide:$index",
                                    "maimai/preview-set",
                                    style = if (index == current.slideSpeed)
                                        RenderData.FILLED_BLUE
                                    else
                                        RenderData.BLUE
                                )
                            }
                        }
                    }
                }
            }
            "menu:star" -> reply("星星头颜色") {
                brief("星星头颜色", "当前 ${starTypeName(current.starType)}")
                keyboard {
                    row {
                        callback(
                            "蓝", "set:star:0", "maimai/preview-set",
                            style = if (current.starType == 0) RenderData.FILLED_BLUE else RenderData.BLUE
                        )
                        callback(
                            "粉", "set:star:1", "maimai/preview-set",
                            style = if (current.starType == 1) RenderData.FILLED_BLUE else RenderData.BLUE
                        )
                    }
                }
            }
            "menu:judge" -> reply("判定样式") {
                line(bold("判定样式"))
                line()
                line("| 名称 | 说明 |")
                line("| --- | --- |")
                judgeStyleNames.forEachIndexed { index, name ->
                    val label = if (index == current.judgeStyle) bold(name) else name
                    line(
                        "| ${href("/mai 设置谱面确认 判定 $name", label)} | " +
                            "![preview #78px #29px]($judgeUrl/$name.png) |"
                    )
                }
            }
            "menu:outline" -> reply("边框样式") {
                line(bold("边框样式"))
                line()
                line("| 名称 | 说明 |")
                line("| --- | --- |")
                outlineStyleNames.forEachIndexed { index, name ->
                    val label = if (index == current.outlineStyle) bold(name) else name
                    line(
                        "| ${href("/mai 设置谱面确认 边框 $name", label)} | " +
                            "![preview #78px #29px]($outlineUrl/${outlineStyleEnumNames[index]}.png) |"
                    )
                }
            }
            "menu:note" -> reply("音符样式") {
                line(bold("音符样式"))
                line()
                line("| 类型 | 样式 |")
                line("| --- | --- |")
                line("| Tap | ${tapStyleNames.mapIndexed { index, name ->
                    val label = if (index == current.tapStyle) bold(name) else name
                    href("/mai 设置谱面确认 Tap $name", label)
                }.joinToString("　")} |")
                line("| Hold | ${holdStyleNames.mapIndexed { index, name ->
                    val label = if (index == current.holdStyle) bold(name) else name
                    href("/mai 设置谱面确认 Hold $name", label)
                }.joinToString("　")} |")
                line("| Slide | ${slideStyleNames.mapIndexed { index, name ->
                    val label = if (index == current.slideStyle) bold(name) else name
                    href("/mai 设置谱面确认 Slide $name", label)
                }.joinToString("　")} |")
            }
            else -> reply("该项暂未开放。")
        }
    }

    private fun formatSlideSpeed(index: Int): String =
        String.format("%.1f", (10 - index) * 0.1f)

    private fun parsePreviewArgument(argument: String, current: MaimaiPreviewSettingsTable.Settings):
        MaimaiPreviewSettingsTable.Settings? {
        val parts = argument.split(" ", limit = 2)
        if (parts.size != 2) return null
        val key = parts[0].trim()
        val raw = parts[1].trim()
        return when (key) {
            "流速" -> raw.toFloatOrNull() ?.takeIf { it in 1.0f..10.0f }
                ?.let { current.copy(noteSpeed = it) }
            "Touch" -> raw.toFloatOrNull() ?.takeIf { it in 1.0f..10.0f }
                ?.let { current.copy(touchSpeed = it) }
            "亮度" -> {
                val number = raw.removeSuffix("%").toFloatOrNull() ?: return null
                val value = if (number <= 1.0f) (number * 100).toInt() else number.toInt()
                if (value in 0..100) current.copy(brightness = value) else null
            }
            "星星时间" -> raw.toFloatOrNull() ?.takeIf { it in -1.0f..1.0f }
                ?.let { current.copy(slideSpeed = Math.round(10 - it * 10).toInt()) }
            "判定" -> judgeStyleIndexOf(raw) ?.let { current.copy(judgeStyle = it) }
            "边框" -> outlineStyleIndexOf(raw) ?.let { current.copy(outlineStyle = it) }
            "Tap" -> tapStyleIndexOf(raw) ?.let { current.copy(tapStyle = it) }
            "Hold" -> holdStyleIndexOf(raw) ?.let { current.copy(holdStyle = it) }
            "Slide" -> slideStyleIndexOf(raw) ?.let { current.copy(slideStyle = it) }
            else -> null
        }
    }

    private fun judgeStyleName(index: Int) =
        if (index in judgeStyleNames.indices) judgeStyleNames[index] else judgeStyleNames[0]

    private fun starTypeName(value: Int) = if (value == 0) "蓝" else "粉"

    private fun noteStyleName(value: Int) = tapStyleName(value)

    private fun tapStyleName(index: Int) =
        if (index in tapStyleNames.indices) tapStyleNames[index] else tapStyleNames[0]

    private fun tapStyleIndexOf(name: String): Int? {
        val index = tapStyleNames.indexOfFirst { it.equals(name, ignoreCase = true) }
        return if (index >= 0) index else null
    }

    private fun holdStyleIndexOf(name: String): Int? {
        val index = holdStyleNames.indexOfFirst { it.equals(name, ignoreCase = true) }
        return if (index >= 0) index else null
    }

    private fun slideStyleIndexOf(name: String): Int? {
        val index = slideStyleNames.indexOfFirst { it.equals(name, ignoreCase = true) }
        return if (index >= 0) index else null
    }

    private fun judgeStyleIndexOf(name: String): Int? {
        val index = judgeStyleNames.indexOfFirst { it.equals(name, ignoreCase = true) }
        if (index >= 0) return index
        val normalized = name.replace("Type", "").replace("-", "").lowercase()
        val aliasIndex = judgeStyleNames.indexOfFirst {
            it.replace("-", "").lowercase() == normalized
        }
        return if (aliasIndex >= 0) aliasIndex else null
    }

    private fun outlineStyleName(index: Int) =
        if (index in outlineStyleNames.indices) outlineStyleNames[index] else outlineStyleNames[0]

    private fun outlineStyleIndexOf(name: String): Int? {
        val index = outlineStyleNames.indexOfFirst { it.equals(name, ignoreCase = true) }
        if (index >= 0) return index
        val enumIndex = outlineStyleEnumNames.indexOfFirst { it.equals(name, ignoreCase = true) }
        return if (enumIndex >= 0) enumIndex else null
    }

    private suspend fun ReplyAble.maimaiPreviewApply(openId: String, payload: String) {
        val parts = payload.split(":")
        if (parts.size != 3) return
        val value = parts[2].toIntOrNull() ?: return
        val current = MaimaiPreviewSettingsTable.settings(openId)
        val updated = when (parts[1]) {
            "slide" -> current.copy(slideSpeed = value)
            "star" -> current.copy(starType = value)
            "note" -> current.copy(tapStyle = value, holdStyle = value, slideStyle = value)
            "tap" -> current.copy(tapStyle = value)
            "hold" -> current.copy(holdStyle = value)
            "noteSlide" -> current.copy(slideStyle = value)
            "judge" -> current.copy(judgeStyle = value)
            "outline" -> current.copy(outlineStyle = value)
            else -> return
        }
        MaimaiPreviewSettingsTable.put(openId, updated)
        reply("设置成功。")
    }

    private fun collection(type: String, id: String) = Keyboard.create {
        row {
            link("选择$type", "https://docs.karenbot.cn/maimai/$id", id = "1")
            at("⚙ 设置$type", "/mai 设置$type ", id = "2")
        }
    }

}